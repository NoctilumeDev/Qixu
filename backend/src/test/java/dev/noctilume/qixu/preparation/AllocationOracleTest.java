package dev.noctilume.qixu.preparation;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static dev.noctilume.qixu.preparation.AllocationGraph.*;

/** Enumerates assignments; deliberately does not implement a matching algorithm. */
class AllocationOracleTest {
    private static final String SEED="ab".repeat(32);
    private static Input graph(int mask,int profile,boolean reverse) {
        var people=new ArrayList<Candidate>();
        for(int p=0;p<4;p++) {
            var edges=new ArrayList<Edge>();
            for(int s=0;s<4;s++)if((mask&(1<<(p*4+s)))!=0)edges.add(new Edge("s"+s,profile<0?1:1+Math.floorMod(p*3+s+profile,3)));
            if(reverse)Collections.reverse(edges);
            people.add(new Candidate("p"+p,edges));
        }
        if(reverse)Collections.reverse(people);
        return new Input(VERSION,reverse?List.of("s3","s2","s1","s0"):List.of("s0","s1","s2","s3"),people);
    }
    @Test void allFourByFourGraphsAgreeWithIndependentFullPreferenceOracle() {
        var order=List.of("p0","p1","p2","p3");
        for(int mask=0;mask<65536;mask++)check(graph(mask,-1,false),order,"presence="+mask);
    }
    @Test void asymmetricRanksAndReorderedContainersAgreeWithIndependentOracle() {
        for(int sample=0;sample<2048;sample++) {
            int mask=(sample*40503+9137)&65535;
            var order=new ArrayList<>(List.of("p0","p1","p2","p3"));
            Collections.rotate(order,sample%4);if((sample&4)!=0)Collections.reverse(order);
            check(graph(mask,sample,false),order,"rank="+sample);
            check(graph(mask,sample,true),order,"reordered="+sample);
        }
    }
    private static String tie(String person,String seat) {
        try {return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(("qixu-seat-order-v2\0"+SEED+"\0"+person+"\0"+seat).getBytes(StandardCharsets.UTF_8)));}
        catch(java.security.NoSuchAlgorithmException e){throw new AssertionError(e);}
    }
    private static void check(Input input,List<String> order,String trace) {
        int[][] score=new int[4][4],ranks=new int[4][4];
        for(int row=0;row<4;row++) {
            Arrays.fill(score[row],-1);String id=order.get(row);
            var raw=input.candidates().stream().filter(c->c.id().equals(id)).findFirst().orElseThrow();
            var choices=new ArrayList<>(raw.acceptable());
            // Independent JCA hashing; no product digest, maximum or augment helper.
            choices.sort(Comparator.comparingInt(Edge::rank).thenComparing(e->tie(id,e.seat())).thenComparing(Edge::seat));
            for(int i=0;i<choices.size();i++){var edge=choices.get(i);int s=Integer.parseInt(edge.seat().substring(1));score[row][s]=i;ranks[row][s]=edge.rank();}
        }
        var oracle=new Enumeration(score);oracle.visit(0,0,0);
        var actual=match(input,order,SEED,System.nanoTime()+20_000_000_000L);
        assertEquals(oracle.bestMatched,actual.maximum(),trace);assertEquals(order,actual.order(),trace);
        assertEquals(4,actual.assignments().size(),trace);var unique=new HashSet<String>();
        for(int row=0;row<4;row++) {
            var assignment=actual.assignments().get(row);int expected=oracle.bestSeats[row];
            assertEquals(order.get(row),assignment.candidate(),trace);
            assertEquals(expected<0?null:"s"+expected,assignment.seat(),trace+" person="+row);
            if(expected>=0){assertTrue(score[row][expected]>=0,trace);assertEquals(ranks[row][expected],assignment.rank(),trace);assertTrue(unique.add(assignment.seat()),trace);}
            else assertNull(assignment.rank(),trace);
        }
        assertEquals(oracle.bestMatched,unique.size(),trace);
    }
    private static final class Enumeration {
        final int[][] scores;final int[] choices=new int[4],vector=new int[4];
        int bestMatched=-1;int[] bestSeats,bestVector;
        Enumeration(int[][] scores){this.scores=scores;}
        void visit(int at,int used,int matched) {
            if(at==4) {
                boolean better=matched>bestMatched;
                if(matched==bestMatched)for(int i=0;i<4;i++)if(vector[i]!=bestVector[i]){better=vector[i]<bestVector[i];break;}
                if(better){bestMatched=matched;bestSeats=choices.clone();bestVector=vector.clone();}
                return;
            }
            choices[at]=-1;vector[at]=4;visit(at+1,used,matched);
            for(int s=0;s<4;s++)if((used&(1<<s))==0&&scores[at][s]>=0){choices[at]=s;vector[at]=scores[at][s];visit(at+1,used|(1<<s),matched+1);}
        }
    }
}
