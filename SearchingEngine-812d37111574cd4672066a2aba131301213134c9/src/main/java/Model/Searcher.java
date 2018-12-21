package Model;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Stream;

public class Searcher {

    Ranker ranker;
    String query;
    String queryAfterParse;
    String[] splitedQueryAfterParse;

    public TreeMap<String,String> Dictionary;
    public HashMap<String, Docs> Documents;
    HashMap<String, QueryDoc> docRelevantForTheQuery;
    PriorityQueue<QueryDoc> RankedQueryDocs;
    HashSet<String> citiesFromFilter; //hashSet for cities if the user chose filter by city
    static double avdl;
    static int numOfDocumentsInCorpus;
    int countDocs;



    public Searcher() {

        docRelevantForTheQuery = new HashMap<String, QueryDoc>();
        RankedQueryDocs = new PriorityQueue();
        ranker = new Ranker();
        //numOfDocumentsInCorpus = Documents.size();
        //citiesFromFilter = new HashSet<String>();
        citiesFromFilter = null;
        Documents = Indexer.docsHashMap;
        countDocs = 0;

    }

    //public void setQuery(ArrayList<QueryTerm> query) {
//        this.query = query;
//    }


    public void setQuery(String query) {
        this.query = query;
    }

    /**
     * Setter for the citiesFromFilter
     *
     * @param cities
     */
    public void setCities(HashSet<String> cities) {
        this.citiesFromFilter = cities;

    }

    public void setDictionary(TreeMap<String, String> dictionary) {
        Dictionary = dictionary;
    }

    public void pasreQuery(String query) throws IOException {

        //init the Documents HashMap from the index
        //loadDocuments();

        //initAvdl
        initAvdl();
        //init the size of the numOfDocumentsInCorpus
        numOfDocumentsInCorpus = Documents.size();
        queryAfterParse = ReadFile.p.parser(null, query, ReadFile.toStem, true);
        splitedQueryAfterParse = queryAfterParse.split(" ");

        for (int i = 0; i < splitedQueryAfterParse.length; i++) {
            String curretTermOfQuery = splitedQueryAfterParse[i];

            initQueryTermAndQueryDocs(curretTermOfQuery);

        }


        Iterator it = docRelevantForTheQuery.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry pair = (Map.Entry) it.next();
            ranker.getQueryDocFromSearcher((QueryDoc)pair.getValue());
            RankedQueryDocs.add((QueryDoc)pair.getValue());
            System.out.println(docRelevantForTheQuery.size());
            System.out.println(pair.getKey());
        }

        /*for (QueryDoc currentQueryDoc : docRelevantForTheQuery) {
            System.out.println(docRelevantForTheQuery.size());
            System.out.println("countDocs= "+countDocs);

            ranker.getQueryDocFromSearcher(currentQueryDoc);
            RankedQueryDocs.add(currentQueryDoc);

        }*/

        int b =0;
        while (!ranker.getqDocQueue().isEmpty() && b<50){
            QueryDoc currentQueryDocFromQueue = (QueryDoc) ranker.getqDocQueue().poll();
            System.out.println(currentQueryDocFromQueue.toString()+System.lineSeparator());
            currentQueryDocFromQueue.setRank(0);
            b++;
        }

        while(!ranker.getqDocQueue().isEmpty()){
            QueryDoc currentQueryDocFromQueue = (QueryDoc) ranker.getqDocQueue().poll();
            currentQueryDocFromQueue.setRank(0);
        }
        docRelevantForTheQuery = new HashMap<>();
        ranker.setqDocQueue(new PriorityQueue<>());

    }




    private void initQueryTermAndQueryDocs(String StringcurretTermOfQuery) {
        QueryTerm currentQueryTerm = null;
        //check if the term exists the dictionary

        //the user load the Dictionary
        if(Indexer.sorted == null){
            if (Dictionary.containsKey(StringcurretTermOfQuery.toLowerCase())) {
                //create a new QueryTerm
                currentQueryTerm = new QueryTerm(StringcurretTermOfQuery.toLowerCase());
            } else {
                //toUpperCase
                if (Dictionary.containsKey(StringcurretTermOfQuery.toUpperCase())) {
                    //create a new QueryTerm
                    currentQueryTerm = new QueryTerm(StringcurretTermOfQuery.toUpperCase());
                }
            }

        }
        //the Dictionary(sorted) is in the memory
        else{
            if (Indexer.sorted.containsKey(StringcurretTermOfQuery.toLowerCase())) {
                //create a new QueryTerm
                currentQueryTerm = new QueryTerm(StringcurretTermOfQuery.toLowerCase());
            } else {
                //toUpperCase
                if (Indexer.sorted.containsKey(StringcurretTermOfQuery.toUpperCase())) {
                    //create a new QueryTerm
                    currentQueryTerm = new QueryTerm(StringcurretTermOfQuery.toUpperCase());
                }
            }

        }

        if (currentQueryTerm != null) {


            //take the term's pointer from the dictionary
            String pointer = Indexer.sorted.get(currentQueryTerm.getValue());
            String[] numOfFileAndLineOfTerm = pointer.split(",");
            String fileNum = numOfFileAndLineOfTerm[0];
            String lineNum = numOfFileAndLineOfTerm[1];
            Integer lineNumInt = Integer.parseInt(lineNum)-1;
            String lineFromFile = "";
            try {
                //doc:FBIS3-29#2=27066 ,27079 doc:FBIS3-5232#1=481 DF- 2 TIC- 3
                lineFromFile = Files.readAllLines(Paths.get(Indexer.pathDir + "\\finalposting" + fileNum + ".txt")).get(lineNumInt);
            } catch (Exception e) {
            }

            //ArrayList<String> docs = new ArrayList<>();
            //ArrayList<Integer> amountsPerDoc = new ArrayList<>();
            String docNo = "";
            String tfString = "";

            //update the hashMap of docs and df of the currentQueryTerm
            for (int k = 0; k < lineFromFile.length(); k++) {

                docNo="";
                tfString="";
                if (lineFromFile.charAt(k) == ':') {
                    countDocs++;
                    k++;

                    //find the doc
                    while (lineFromFile.charAt(k) != '#') {
                        docNo = docNo + lineFromFile.charAt(k);
                        if (docNo.equals("FBIS3-947")){
                            System.out.println("debug");
                        }
                        k++;
                    }
                    k++;

                    //find the amountAppearence in the doc
                    while (lineFromFile.charAt(k) != '=') {
                        tfString = tfString + lineFromFile.charAt(k);
                        k++;
                    }

                    int tf = Integer.parseInt(tfString);

                    if (Documents.containsKey(docNo)) {
                        Docs docFromOriginalDocs = Documents.get(docNo);

                        //if there is filter by city
                        if (citiesFromFilter != null) {
                            for (String city : citiesFromFilter) {

                                //the doc's city included the filter
                                if (city.equals(docFromOriginalDocs.getCity())) {
                                    //add the doc to the QueryTerm
                                    currentQueryTerm.getDocsAndAmount().put(docNo, tf);
                                    //add the QueryTerm to the relevant doc
                                    QueryDoc newQueryDoc = new QueryDoc(docFromOriginalDocs.getDocNo());
                                    newQueryDoc.setLength(docFromOriginalDocs.getDocLength());
                                    //add the QueryTerm to the relevant doc
                                    newQueryDoc.getQueryTermsInDocsAndQuery().put(currentQueryTerm.getValue(),currentQueryTerm);
                                    //add the new QueryDoc to the HashSet of the relevant docs for the query
                                    if (!docRelevantForTheQuery.containsKey(newQueryDoc.getDocNO()))
                                        docRelevantForTheQuery.put(newQueryDoc.getDocNO(),newQueryDoc);


                                }
                            }

                            //there is no filter by city
                        } else {


                            //add the doc to the QueryTerm
                            currentQueryTerm.getDocsAndAmount().put(docNo, tf);

                            QueryDoc newQueryDoc = new QueryDoc(docFromOriginalDocs.getDocNo());
                            //set the length of the relevant doc
                            newQueryDoc.setLength(docFromOriginalDocs.getDocLength());
                            //add the QueryTerm to the relevant doc
                            newQueryDoc.getQueryTermsInDocsAndQuery().put(currentQueryTerm.getValue(),currentQueryTerm);
                            //add the new QueryDoc to the HashSet of the relevant docs for the query
                            if (!docRelevantForTheQuery.containsKey(newQueryDoc.getDocNO()))
                                docRelevantForTheQuery.put(newQueryDoc.getDocNO(),newQueryDoc);



                        }

                    }


                }
                if (lineFromFile.charAt(k) == 'D' && k + 5 < lineFromFile.length() &&
                        lineFromFile.charAt(k + 1) == 'F' && lineFromFile.charAt(k + 2) == '-' &&
                        lineFromFile.charAt(k + 3) == ' ') {

                    String df = "";
                    int q = 4;
                    while (k + q < lineFromFile.length()) {
                        if (lineFromFile.charAt(k + q) != ' ') {
                            df = df + lineFromFile.charAt(k + q);
                            break;
                        }
                        q++;

                    }

                    try {
                        Integer dfInt = Integer.parseInt(df);
                        currentQueryTerm.setDf(dfInt);
                    } catch (Exception e) {
                    }

                }


            }

            //update the amount of appearence in the query
            for (int i = 0; i < splitedQueryAfterParse.length; i++) {

                if (splitedQueryAfterParse[i].equals(StringcurretTermOfQuery)) {
                    currentQueryTerm.setAppearanceInQuery(currentQueryTerm.getAppearanceInQuery() + 1);

                }


            }

        }

    }

    private void initAvdl() {
        Integer countDocsLength = 0;
        Iterator it = Documents.entrySet().iterator();
        while (it.hasNext()) {
            //Terms nextTerm = (Terms) it.next();
            //text.append(nextTerm.getValue());
            Map.Entry pair = (Map.Entry) it.next();
            countDocsLength = countDocsLength + ((Docs) pair.getValue()).getDocLength();
        }
        avdl = countDocsLength / Documents.size();
    }

    private void loadDocuments() {

        try {

            FileInputStream f = new FileInputStream(new File(Indexer.pathDir + "\\" + "DocsAsObject.txt"));

            ObjectInputStream o = new ObjectInputStream(f);
            Documents = (HashMap<String, Docs>) o.readUnshared();
            o.close();

        } catch (Exception e) {
        }


    }


}