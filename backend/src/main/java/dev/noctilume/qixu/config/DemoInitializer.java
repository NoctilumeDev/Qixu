package dev.noctilume.qixu.config;

import java.util.Map;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

@Component
public class DemoInitializer implements ApplicationRunner {
    private final JdbcTemplate jdbc;
    private final JsonMapper json;
    private final Environment environment;
    private final boolean enabled;
    public DemoInitializer(JdbcTemplate jdbc,JsonMapper json,Environment environment,@Value("${qixu.demo-enabled:false}") boolean enabled) { this.jdbc=jdbc; this.json=json; this.environment=environment; this.enabled=enabled; }
    @Override @Transactional public void run(ApplicationArguments args) {
        if (!enabled) return;
        Set<String> profiles=Set.of(environment.getActiveProfiles());
        if (!profiles.contains("demo") || profiles.contains("prod") || profiles.contains("production")) throw new IllegalStateException("Demo identities require demo profile and cannot run in production");
        var encoder=new BCryptPasswordEncoder(12);
        String[][] users={{"student1","林同学","STUDENT"},{"student2","陈同学","STUDENT"},{"teacher1","周老师","TEACHER"},{"admin1","空间管理员","ADMIN"},{"admin2","三楼管理员","ADMIN"}};
        for(int i=0;i<users.length;i++) {
            if (jdbc.queryForObject("SELECT COUNT(*) FROM identity_user WHERE username=?",Long.class,users[i][0])==0) {
                jdbc.update("INSERT INTO identity_user(id,username,password_hash,display_name,role,student_verified) VALUES(?,?,?,?,?,?)",i+1,users[i][0],encoder.encode("qixu-demo"),users[i][1],users[i][2],users[i][2].equals("STUDENT"));
            }
        }
        jdbc.update("INSERT IGNORE INTO floor(id,name,building,level_number) VALUES(100,'三楼 · 学习与研讨','图书馆',3),(101,'四楼 · 普通自习','图书馆',4)");
        jdbc.update("INSERT IGNORE INTO admin_scope(user_id,floor_id) VALUES(4,100),(4,101),(5,100)");
        area(1000,100,"WEST","西区 · 普通自习","BOOKABLE",24,30,40,430,630);
        area(1001,100,"EAST","东区 · 备考座位","PREPARATION",24,510,40,430,630);
        area(1002,101,"NORTH","普通自习区","BOOKABLE",12,40,40,900,600);
        for(int i=0;i<24;i++) {
            int x=65+(i%6)*62,y=90+(i/6)*125;
            space(2000+i,100,1000L,"A"+String.format("%03d",i+1),"西区单人位 "+(i+1),"SEAT","BOOKABLE",1,x,y,42,60,"window-seat",i%6==0,true,i%4==0);
            space(2100+i,100,1001L,"B"+String.format("%03d",i+1),"东区备考位 "+(i+1),"SEAT","PREPARATION",1,x+480,y,42,60,"window-seat",i%6==5,true,true);
        }
        for(int i=0;i<12;i++) space(2200+i,101,1002L,"C"+String.format("%03d",i+1),"四楼普通位 "+(i+1),"SEAT","BOOKABLE",1,80+(i%6)*125,100+(i/6)*180,55,70,"window-seat",i%6==0,i%2==0,false);
        space(3000,100,null,"R-A","研讨室 A","ROOM","VENUE",8,40,760,210,160,"quiet-room",true,true,true);
        space(3001,100,null,"R-B","研讨室 B","ROOM","VENUE",24,280,760,210,160,"quiet-room",false,true,true);
        space(3002,100,null,"H-A","报告厅 A","HALL","VENUE",180,550,740,370,200,"hall",false,true,false);
    }
    private void area(long id,long floor,String code,String name,String mode,int capacity,int x,int y,int w,int h) { space(id,floor,null,code,name,"AREA",mode,capacity,x,y,w,h,null,false,false,true); }
    private void space(long id,long floor,Long parent,String code,String name,String kind,String mode,int capacity,int x,int y,int w,int h,String image,boolean window,boolean outlet,boolean quiet) {
        var profile=Map.of("source","DEMO","notice","演示空间与示意图片，不代表实际校园测量。","features",Map.of("window",window,"outlet",outlet,"quiet",quiet,"accessible",true),"description",kind.equals("SEAT")?"单人学习空间，查看公开设施条件后选择。":"供阅读交流与校园公开活动使用。","lighting",window?"自然光较好（演示描述）":"均匀室内照明（演示描述）","dimensions",kind.equals("SEAT")?"80 × 60 cm（演示）":"以实际现场确认容量为准","airflow","未采集","reviewedAt","2026-10-03");
        jdbc.update("INSERT IGNORE INTO space(id,floor_id,parent_id,code,name,kind,use_mode,capacity,map_x,map_y,map_w,map_h,image_key,profile_json) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?)",id,floor,parent,code,name,kind,mode,capacity,x,y,w,h,image,json.writeValueAsString(profile));
    }
}
