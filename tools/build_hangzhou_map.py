#!/usr/bin/env python3
"""Build a compact, styled OSM scene pack from Geofabrik PBF."""
import argparse, hashlib, math, os, sqlite3, struct
from datetime import datetime, timezone
import osmium

ROAD_CLASSES = {
    "motorway": 0, "motorway_link": 0, "trunk": 0, "trunk_link": 0,
    "primary": 1, "primary_link": 1, "secondary": 2,
    "secondary_link": 2, "tertiary": 2, "tertiary_link": 2,
    "unclassified": 3, "residential": 3, "living_street": 3,
    "service": 4,
}
DENIED = {"no", "private"}
AREA_CLASSES = {
    ("natural", "water"): 0, ("water", None): 0,
    ("landuse", "forest"): 1, ("natural", "wood"): 1,
    ("leisure", "park"): 2, ("leisure", "garden"): 2,
    ("landuse", "grass"): 3, ("natural", "grassland"): 3,
    ("landuse", "residential"): 4, ("landuse", "commercial"): 5,
    ("landuse", "industrial"): 5,
}

def perpendicular_sq(point, start, end, cos_lat):
    scale_x = 111_320.0 * cos_lat
    px, py = point[1] * scale_x, point[0] * 111_320.0
    ax, ay = start[1] * scale_x, start[0] * 111_320.0
    bx, by = end[1] * scale_x, end[0] * 111_320.0
    dx, dy = bx - ax, by - ay
    if dx == 0 and dy == 0: return (px-ax)**2 + (py-ay)**2
    t = max(0.0, min(1.0, ((px-ax)*dx + (py-ay)*dy)/(dx*dx+dy*dy)))
    qx, qy = ax+t*dx, ay+t*dy
    return (px-qx)**2 + (py-qy)**2

def simplify(points, tolerance, cos_lat):
    if len(points) <= 2: return points
    keep = {0, len(points)-1}; stack = [(0, len(points)-1)]; limit = tolerance*tolerance
    while stack:
        first, last = stack.pop(); best, best_d = -1, limit
        for i in range(first+1, last):
            d = perpendicular_sq(points[i], points[first], points[last], cos_lat)
            if d > best_d: best, best_d = i, d
        if best >= 0: keep.add(best); stack.extend(((first,best),(best,last)))
    return [p for i,p in enumerate(points) if i in keep]

def encode(points):
    return b"".join(struct.pack("<ii", round(lat*1_000_000), round(lon*1_000_000)) for lat,lon in points)

def polygon_centroid(points):
    """Return the area-weighted centroid; fall back to the average for degenerate rings."""
    twice_area=cx=cy=0.0
    for i,(lat1,lon1) in enumerate(points):
        lat2,lon2=points[(i+1)%len(points)]; cross=lon1*lat2-lon2*lat1
        twice_area+=cross; cx+=(lon1+lon2)*cross; cy+=(lat1+lat2)*cross
    if abs(twice_area)<1e-14:
        return (sum(p[0] for p in points)/len(points),sum(p[1] for p in points)/len(points))
    return (cy/(3*twice_area),cx/(3*twice_area))

def point_inside(point, ring):
    lat,lon=point; inside=False
    for i,(a_lat,a_lon) in enumerate(ring):
        b_lat,b_lon=ring[(i+1)%len(ring)]
        if (a_lat>lat)!=(b_lat>lat):
            crossing=(b_lon-a_lon)*(lat-a_lat)/(b_lat-a_lat)+a_lon
            if lon<crossing: inside=not inside
    return inside

def visual_label_point(points, cos_lat):
    """Approximate a polylabel point: inside the polygon and far from every edge."""
    center=polygon_centroid(points)
    min_lat,max_lat=min(p[0] for p in points),max(p[0] for p in points)
    min_lon,max_lon=min(p[1] for p in points),max(p[1] for p in points)
    best=center if point_inside(center,points) else points[0]; best_score=-1.0
    lo_lat,hi_lat,lo_lon,hi_lon=min_lat,max_lat,min_lon,max_lon
    for _ in range(3):
        for yi in range(8):
            lat=lo_lat+(hi_lat-lo_lat)*(yi+.5)/8
            for xi in range(8):
                lon=lo_lon+(hi_lon-lo_lon)*(xi+.5)/8; q=(lat,lon)
                if not point_inside(q,points): continue
                score=min(perpendicular_sq(q,points[j],points[(j+1)%len(points)],cos_lat) for j in range(len(points)))
                if score>best_score: best,best_score=q,score
        span_lat=(hi_lat-lo_lat)/8; span_lon=(hi_lon-lo_lon)/8
        lo_lat,hi_lat=best[0]-span_lat,best[0]+span_lat; lo_lon,hi_lon=best[1]-span_lon,best[1]+span_lon
    return best

class Builder(osmium.SimpleHandler):
    def __init__(self, db, bbox):
        super().__init__(); self.db=db; self.min_lon,self.min_lat,self.max_lon,self.max_lat=bbox
        self.cos_lat=math.cos(math.radians((self.min_lat+self.max_lat)/2)); self.roads=0; self.buildings=0; self.areas=0; self.lines=0; self.labels=0

    def node(self, node):
        name=node.tags.get("name")
        if not name or not node.location.valid(): return
        lat,lon=node.location.lat,node.location.lon
        if lon<self.min_lon or lon>self.max_lon or lat<self.min_lat or lat>self.max_lat: return
        if not any(node.tags.get(k) for k in ("amenity","shop","tourism","office","healthcare","leisure","place")): return
        self.insert_label(2_000_000_000_000+node.id,name,1,(lat,lon)); self.labels+=1

    def way(self, way):
        highway=way.tags.get("highway"); is_building=bool(way.tags.get("building")) and way.tags.get("building")!="no"
        area_class=None
        for (key,value),cls in AREA_CLASSES.items():
            if way.tags.get(key) and (value is None or way.tags.get(key)==value): area_class=cls; break
        line_class=0 if way.tags.get("waterway") else (1 if way.tags.get("railway") in ("rail","subway","light_rail") else None)
        if highway not in ROAD_CLASSES and not is_building and area_class is None and line_class is None: return
        try: points=[(n.location.lat,n.location.lon) for n in way.nodes if n.location.valid()]
        except osmium.InvalidLocationError: return
        if len(points)<2: return
        if max(p[1] for p in points)<self.min_lon or min(p[1] for p in points)>self.max_lon or max(p[0] for p in points)<self.min_lat or min(p[0] for p in points)>self.max_lat: return
        if highway in ROAD_CLASSES and not any(way.tags.get(k) in DENIED for k in ("access","vehicle","motor_vehicle")):
            pts=simplify(points,2.5,self.cos_lat)
            if len(pts)>=2:
                self.insert("roads",way.id,ROAD_CLASSES[highway],pts,way.tags.get("name")); self.roads+=1
                if way.tags.get("name"): self.insert_label(way.id,way.tags.get("name"),2,pts[len(pts)//2]); self.labels+=1
        if is_building and len(points)>=4 and points[0]==points[-1]:
            ring=points[:-1]; split=max(range(1,len(ring)),key=lambda i:perpendicular_sq(ring[i],ring[0],ring[0],self.cos_lat),default=1)
            ring=simplify(ring[:split+1],1.5,self.cos_lat)+simplify(ring[split:]+[ring[0]],1.5,self.cos_lat)[1:-1]
            if len(ring)>=3:
                self.insert("buildings",way.id,1 if way.tags.get("historic") or way.tags.get("tourism")=="attraction" else 0,ring,way.tags.get("name")); self.buildings+=1
                if way.tags.get("name"): self.insert_label(-way.id,way.tags.get("name"),0,visual_label_point(ring,self.cos_lat)); self.labels+=1
        if area_class is not None and len(points)>=4 and points[0]==points[-1]:
            ring=simplify(points[:-1],2.5,self.cos_lat)
            if len(ring)>=3:
                self.insert("areas",way.id,area_class,ring,way.tags.get("name")); self.areas+=1
                if way.tags.get("name"): self.insert_label(1_000_000_000_000+way.id,way.tags.get("name"),0,visual_label_point(ring,self.cos_lat)); self.labels+=1
        if line_class is not None:
            pts=simplify(points,2.0,self.cos_lat)
            if len(pts)>=2: self.insert("lines",way.id,line_class,pts,way.tags.get("name")); self.lines+=1

    def insert(self, table, osm_id, cls, points, name=None):
        lats=[round(p[0]*1_000_000) for p in points]; lons=[round(p[1]*1_000_000) for p in points]
        cur=self.db.execute(f"INSERT INTO {table}(osm_way_id,name,class,points,min_lat_e6,max_lat_e6,min_lon_e6,max_lon_e6) VALUES(?,?,?,?,?,?,?,?)",
            (osm_id,name,cls,encode(points),min(lats),max(lats),min(lons),max(lons)))
        row=cur.lastrowid; self.db.execute(f"INSERT INTO {table[:-1]}_rtree VALUES(?,?,?,?,?)",(row,min(lats),max(lats),min(lons),max(lons)))

    def insert_label(self, osm_id, name, cls, point):
        self.db.execute("INSERT OR IGNORE INTO labels(osm_id,name,class,lat_e6,lon_e6) VALUES(?,?,?,?,?)",
            (osm_id,name,cls,round(point[0]*1_000_000),round(point[1]*1_000_000)))

def create_schema(db):
    db.executescript("""
    PRAGMA journal_mode=OFF; PRAGMA synchronous=OFF; PRAGMA user_version=2;
    CREATE TABLE metadata(key TEXT PRIMARY KEY,value TEXT NOT NULL);
    CREATE TABLE roads(id INTEGER PRIMARY KEY,osm_way_id INTEGER,name TEXT,class INTEGER NOT NULL,points BLOB NOT NULL,min_lat_e6 INTEGER,max_lat_e6 INTEGER,min_lon_e6 INTEGER,max_lon_e6 INTEGER);
    CREATE VIRTUAL TABLE road_rtree USING rtree(id,min_lat_e6,max_lat_e6,min_lon_e6,max_lon_e6);
    CREATE TABLE buildings(id INTEGER PRIMARY KEY,osm_way_id INTEGER,name TEXT,class INTEGER NOT NULL,points BLOB NOT NULL,min_lat_e6 INTEGER,max_lat_e6 INTEGER,min_lon_e6 INTEGER,max_lon_e6 INTEGER);
    CREATE VIRTUAL TABLE building_rtree USING rtree(id,min_lat_e6,max_lat_e6,min_lon_e6,max_lon_e6);
    CREATE TABLE areas(id INTEGER PRIMARY KEY,osm_way_id INTEGER,name TEXT,class INTEGER NOT NULL,points BLOB NOT NULL,min_lat_e6 INTEGER,max_lat_e6 INTEGER,min_lon_e6 INTEGER,max_lon_e6 INTEGER);
    CREATE VIRTUAL TABLE area_rtree USING rtree(id,min_lat_e6,max_lat_e6,min_lon_e6,max_lon_e6);
    CREATE TABLE lines(id INTEGER PRIMARY KEY,osm_way_id INTEGER,name TEXT,class INTEGER NOT NULL,points BLOB NOT NULL,min_lat_e6 INTEGER,max_lat_e6 INTEGER,min_lon_e6 INTEGER,max_lon_e6 INTEGER);
    CREATE VIRTUAL TABLE line_rtree USING rtree(id,min_lat_e6,max_lat_e6,min_lon_e6,max_lon_e6);
    CREATE TABLE labels(id INTEGER PRIMARY KEY,osm_id INTEGER UNIQUE,name TEXT NOT NULL,class INTEGER NOT NULL,lat_e6 INTEGER NOT NULL,lon_e6 INTEGER NOT NULL);
    CREATE INDEX label_position ON labels(lat_e6,lon_e6);
    """)

def main():
    ap=argparse.ArgumentParser(); ap.add_argument("input"); ap.add_argument("output"); ap.add_argument("--bbox",default="119.95,30.10,120.35,30.40"); args=ap.parse_args()
    bbox=tuple(map(float,args.bbox.split(","))); os.makedirs(os.path.dirname(args.output),exist_ok=True)
    if os.path.exists(args.output): os.unlink(args.output)
    db=sqlite3.connect(args.output); create_schema(db); builder=Builder(db,bbox)
    builder.apply_file(args.input,locations=True,idx="flex_mem");
    digest=hashlib.sha256(open(args.input,"rb").read()).hexdigest()
    metadata={"schema_version":"2","coordinate_system":"WGS84","region":"Hangzhou Core","bbox":args.bbox,"provider":"OpenStreetMap via Geofabrik Zhejiang","attribution":"© OpenStreetMap contributors","source_sha256":digest,"built_at":datetime.now(timezone.utc).isoformat(),"roads":str(builder.roads),"buildings":str(builder.buildings),"areas":str(builder.areas),"lines":str(builder.lines),"labels":str(builder.labels)}
    db.executemany("INSERT INTO metadata VALUES(?,?)",metadata.items()); db.commit(); db.execute("VACUUM"); db.close()
    print(f"roads={builder.roads} buildings={builder.buildings} areas={builder.areas} lines={builder.lines} labels={builder.labels} bytes={os.path.getsize(args.output)}")

if __name__=="__main__": main()
