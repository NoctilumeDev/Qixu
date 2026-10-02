package dev.noctilume.qixu.preparation;

import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static dev.noctilume.qixu.preparation.AllocationGraph.*;

class AllocationGraphTest {
    private static final String HASH="ab".repeat(32),RANDOM="cd".repeat(32);
    private static Candidate person(String id,Edge... edges) {return new Candidate(id,List.of(edges));}
    private static Input input(List<String> seats,Candidate... people) {return new Input(VERSION,seats,List.of(people));}
    @Test void flexibleFirstCannotManufactureScarcity() {
        var graph=input(List.of("Q","N"),person("a",new Edge("Q",1),new Edge("N",2)),person("b",new Edge("Q",1)));
        var result=match(graph,List.of("a","b"),HASH,System.nanoTime()+20_000_000_000L);
        assertEquals(2,result.maximum()); assertEquals("N",result.assignments().get(0).seat());assertEquals("Q",result.assignments().get(1).seat());
        var hard=input(List.of("Q","N"),person("a",new Edge("Q",1)),person("b",new Edge("Q",1)));
        var scarce=match(hard,List.of("a","b"),HASH,System.nanoTime()+20_000_000_000L);
        assertEquals(1,scarce.maximum());assertNull(scarce.assignments().get(1).seat());
    }
    @Test void allThreeByThreeGraphsAgreeWithIndependentExhaustiveOracle() {
        // The oracle enumerates every assignment including unmatched, rather than calling augment().
        for(int mask=0;mask<512;mask++) {
            var people=new ArrayList<Candidate>();
            for(int p=0;p<3;p++) {var edges=new ArrayList<Edge>();for(int s=0;s<3;s++)if((mask&(1<<(p*3+s)))!=0)edges.add(new Edge("s"+s,s+1));people.add(new Candidate("p"+p,edges));}
            var graph=new Input(VERSION,List.of("s0","s1","s2"),people);
            for(var order:List.of(List.of("p0","p1","p2"),List.of("p2","p0","p1"),List.of("p1","p2","p0"))) {
                var result=match(graph,order,HASH,System.nanoTime()+20_000_000_000L);
                var oracle=exhaust(graph,order,0,new HashSet<>(),new ArrayList<>());
                assertEquals(oracle.matched,result.maximum(),"mask="+mask);
                assertEquals(oracle.ranks,result.assignments().stream().map(a->a.rank()==null?4:a.rank()).toList(),"priority mask="+mask);
            }
        }
    }
    private record Oracle(int matched,List<Integer> ranks) {}
    private static Oracle exhaust(Input input,List<String> order,int at,Set<String> used,List<Integer> ranks) {
        if(at==order.size())return new Oracle(used.size(),List.copyOf(ranks));
        var candidate=input.candidates().stream().filter(c->c.id().equals(order.get(at))).findFirst().orElseThrow();
        ranks.add(4); Oracle best=exhaust(input,order,at+1,used,ranks);ranks.remove(ranks.size()-1);
        for(var edge:candidate.acceptable())if(used.add(edge.seat())) {
            ranks.add(edge.rank());var next=exhaust(input,order,at+1,used,ranks);ranks.remove(ranks.size()-1);used.remove(edge.seat());
            if(better(next,best))best=next;
        }
        return best;
    }
    private static boolean better(Oracle a,Oracle b) {
        if(a.matched!=b.matched)return a.matched>b.matched;
        for(int i=0;i<a.ranks.size();i++)if(!a.ranks.get(i).equals(b.ranks.get(i)))return a.ranks.get(i)<b.ranks.get(i);
        return false;
    }
    @Test void frozenOrderingIsIndependentOfInputContainerOrderAndNeverOversells() {
        var graph=input(List.of("Q","N"),person("a",new Edge("Q",1),new Edge("N",1)),person("b",new Edge("Q",1)),person("c"));
        var reordered=input(List.of("N","Q"),person("c"),person("b",new Edge("Q",1)),person("a",new Edge("N",1),new Edge("Q",1)));
        assertEquals(allocate(graph,HASH,RANDOM),allocate(reordered,HASH,RANDOM));
        var result=allocate(graph,HASH,RANDOM);
        assertEquals(2,result.maximum());assertEquals(2,result.assignments().stream().map(Assignment::seat).filter(Objects::nonNull).distinct().count());
        assertEquals(3,result.assignments().size());
    }
    @Test void hundredApplicantsHaveRealFallbackAndBoundedShortage() {
        for(int count:List.of(110,80)) {
            var seats=new ArrayList<String>();for(int i=0;i<count;i++)seats.add("s"+i);
            var people=new ArrayList<Candidate>();
            for(int i=0;i<100;i++){var edges=new ArrayList<Edge>();for(int s=0;s<count;s++)edges.add(new Edge("s"+s,s<30?1:2));people.add(new Candidate("p"+i,edges));}
            var result=allocate(new Input(VERSION,seats,people),HASH,RANDOM);
            assertEquals(Math.min(100,count),result.maximum());assertEquals(100,result.assignments().size());
            assertEquals(Math.max(0,100-count),result.assignments().stream().filter(a->a.seat()==null).count());
        }
    }
    @Test void malformedGraphAndExhaustedBudgetDoNotProduceCandidateResult() {
        assertThrows(IllegalArgumentException.class,()->allocate(input(List.of("Q","Q"),person("a")),HASH,RANDOM));
        assertThrows(IllegalArgumentException.class,()->allocate(input(List.of("Q"),person("a",new Edge("N",1))),HASH,RANDOM));
        assertThrows(IllegalArgumentException.class,()->allocate(input(List.of("Q"),person("a",new Edge("Q",1),new Edge("Q",2))),HASH,RANDOM));
        assertThrows(IllegalStateException.class,()->match(input(List.of("Q"),person("a",new Edge("Q",1))),List.of("a"),HASH,System.nanoTime()-1));
    }
}
