package dev.noctilume.qixu.spaces;

import dev.noctilume.qixu.common.DomainException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import tools.jackson.databind.json.JsonMapper;

@Service
public class SpaceService {
    private final JdbcTemplate jdbc;
    private final JsonMapper json;
    public record Space(long id,long floorId,Long parentId,String code,String name,String kind,String useMode,int capacity,long version,int x,int y,int width,int height,String imageKey,Map<?,?> profile,String availability) {}
    public SpaceService(JdbcTemplate jdbc,JsonMapper json) { this.jdbc=jdbc; this.json=json; }
    private Space map(java.sql.ResultSet rs,int row) throws java.sql.SQLException {
        return new Space(rs.getLong("id"),rs.getLong("floor_id"),rs.getObject("parent_id",Long.class),rs.getString("code"),rs.getString("name"),rs.getString("kind"),rs.getString("use_mode"),rs.getInt("capacity"),rs.getLong("version"),rs.getInt("map_x"),rs.getInt("map_y"),rs.getInt("map_w"),rs.getInt("map_h"),rs.getString("image_key"),json.readValue(rs.getString("profile_json"),Map.class),"NOT_QUERIED");
    }
    public List<Map<String,Object>> floors() { return jdbc.queryForList("SELECT id,name,building,level_number AS levelNumber,version,source_kind AS sourceKind FROM floor ORDER BY building,level_number"); }
    public Map<String,Object> list(Long floor,String kind,String search,String tag,int page,int size) {
        if (page<1 || page>10000 || size<1 || size>50) throw DomainException.invalid("分页参数超出范围。");
        if (search!=null && search.length()>80) throw DomainException.invalid("搜索内容过长。");
        StringBuilder where=new StringBuilder(" WHERE active=TRUE"); var args=new ArrayList<Object>();
        if (floor!=null) { where.append(" AND floor_id=?"); args.add(floor); }
        if (kind!=null && !kind.isBlank()) {
            if (!List.of("AREA","SEAT","ROOM","HALL").contains(kind)) throw DomainException.invalid("空间类型不正确。");
            where.append(" AND kind=?"); args.add(kind);
        }
        if (search!=null && !search.isBlank()) {
            where.append(" AND (name LIKE ? ESCAPE '!' OR code LIKE ? ESCAPE '!')");
            String match="%"+search.replace("!","!!").replace("%","!%").replace("_","!_")+"%"; args.add(match); args.add(match);
        }
        if (tag!=null && !tag.isBlank()) {
            if (!List.of("window","outlet","quiet","accessible").contains(tag)) throw DomainException.invalid("设施筛选项不正确。");
            where.append(" AND JSON_UNQUOTE(JSON_EXTRACT(profile_json,?))='true'"); args.add("$.features."+tag);
        }
        long total=jdbc.queryForObject("SELECT COUNT(*) FROM space"+where,Long.class,args.toArray());
        args.add(size); args.add((page-1)*size);
        var items=jdbc.query("SELECT * FROM space"+where+" ORDER BY floor_id,code LIMIT ? OFFSET ?",this::map,args.toArray());
        return Map.of("items",items,"total",total,"page",page,"size",size);
    }
    public Space get(long id) {
        var items=jdbc.query("SELECT * FROM space WHERE id=? AND active=TRUE",this::map,id);
        if (items.isEmpty()) throw DomainException.missing();
        return items.get(0);
    }
}
