import java.util.*;
import java.text.SimpleDateFormat;
import java.text.ParseException;

public class Predicates{

    private FreebaseDBHandler db;
    private Map<String,List<NTriple>> allPreds = new HashMap<>(); 
    private String freebaseID;

    public Predicates(String freebaseID, FreebaseDBHandler db ){
        this.db = db;
        this.freebaseID = freebaseID;
        ID2Predicates ();
    }


    //returns all the triples of this ID based on predicate
    private void ID2Predicates () { 
        List<NTriple> tagTriples = new ArrayList<>();
        tagTriples = db.ID2Triples(this.freebaseID, tagTriples);
        List<NTriple> temp2 = new ArrayList<>();

        if (tagTriples.size() == 0)
            return;
        for (NTriple p: tagTriples){
            if(allPreds == null)
                return;
            temp2 = this.allPreds.get(p.getPredicate());
            if (temp2 == null)
                temp2 = new ArrayList<>();
            temp2.add(p);
            this.allPreds.put(p.getPredicate(),temp2);
        }
        tagTriples.clear();
    }

    public List<NTriple> getPredObjects (String predicate, List<NTriple> objectTriples) { 

        if(!this.allPreds.keySet().contains(predicate))
            return null;

        List<NTriple> temp2 = this.allPreds.get(predicate);

        if (temp2.size() == 0)
            return null;

        return temp2;
    }

    public List<NTriple> sortPredObjects (String predicate, List<NTriple> triples) { // triples are the initial ones, e.g. all the spouses
        //triples are output of getPredObjects
        List<NTriple> results = new ArrayList<>();
        List<NTriple> predTriples = new ArrayList<>(); // the triples containing the objects and specific predicate we want to sort
        if(triples == null)
            return null;

        for(NTriple p: triples){
            predTriples = db.ID2TriplesFull(p.getObjectID(), predTriples);
            for(NTriple t: predTriples){
                if(t.getPredicate().equals(predicate)){
                    t.isStringNTriple();
                    results.add(t);
                }
            }
            predTriples.clear();
        }

        MergeSort.sort(results, 0, results.size()-1);
        return results; //now it's sorted
    }

    public NTriple argMax (String predicate, List<NTriple> triples) { 
        List<NTriple> predTriples = sortPredObjects(predicate,triples);
        if (predTriples == null)
            return null;
        if (predTriples.size() == 0)
            return null;
        return predTriples.get(predTriples.size()-1);
    }

    public NTriple argMin (String predicate, List<NTriple> triples) { 
        List<NTriple> predTriples = sortPredObjects(predicate,triples);
        if (predTriples == null)
            return null;
        if (predTriples.size() == 0)
            return null;
        return predTriples.get(0);
    }

    public int searchForObject (String objectID, List<NTriple> sortedTriples) { // triples are the initial ones, e.g. all the spouses.
        if(sortedTriples == null)
            return -1;

        for(NTriple p: sortedTriples){
            if(p.getSubjectID().equals(objectID))
                return sortedTriples.indexOf(p);
        }
        return -1;
    }

    public int countPredicate (String entry){
        return this.allPreds.get(entry).size();
    }

    // a triple is comparable if there is more than one triple with the same predicate. 
    // AND (objects connected to that triple are either mediators which then have to have predicates which are numbers/dates
    // OR objects are normal triples and have a predicate that is number/date.
    public void isTripleComparable(String predicate){
        if(countPredicate(predicate)> 1){
            System.out.println("    "+predicate+"       --> " + countPredicate(predicate));
            // List<NTriple> tagTriples = new ArrayList<>();
            // tagTriples = db.ID2Triples(this.allPreds.get(predicate).get(0).getObjectID(), tagTriples);
            // if(tagTriples.size()>0)
            //     System.out.println("        "+tagTriples);
        } else {
            return;
        }
        List<NTriple> subjTriples2 = this.allPreds.get(predicate);
        List<NTriple> objTriples = new ArrayList<>();
        List<NTriple> objStringTriples = new ArrayList<>();

        // we only need to check for one of them to know if it's possible to sort
        objTriples = db.ID2Triples(subjTriples2.get(0).getObjectID(), objTriples);
        objStringTriples = db.ID2Triples(subjTriples2.get(0).getObjectID(), objStringTriples);
        for (NTriple c: objTriples){
            if (c.getObjectID() == subjTriples2.get(0).getSubjectID()){
                continue;
            }
            System.out.println(c.getObjectID());
        }
        for (NTriple c: objStringTriples){
            if(c.getObjectID().matches(".*\\d+.*"))
                System.out.println(c.getObjectID());
        }
    }

    public void printPredicate (){
        Set<String> objectIDs = new HashSet<>();
        if (this.allPreds == null)
            return;
        if (this.allPreds.size() == 0)
            return;
        for (String entry: this.allPreds.keySet()){
            objectIDs = new HashSet<>();
            for (NTriple t: this.allPreds.get(entry)){
                objectIDs.add(t.getObjectID());
            }
        }
    }
}