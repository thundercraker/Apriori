package org.yumashish.apriori;

import java.io.*;
import java.util.*;

/**
 * Created by Yumashish on 4/29/15.
 */
public class Apriori {

    public static void main(String[] args)
    {

        String file = args[0];

        Apriori ap = new Apriori(file, 40, 0.7d);

        //ap.CountOccurance(ap.GetTransaction(2));
        ap.Execute(true);
    }

    String[] names;
    Set<Integer> items;
    List<HashSet<Integer>> transactions;
    int support;
    double confidence;
    boolean readError = false;

    public Apriori(String file, int support, double confidence)
    {
        this.support = support;
        this.confidence = confidence;
        this.items = new HashSet<>();
        this.transactions = new ArrayList<>();

        try
        {
            ReadDataSetCSV(file);
        } catch (IOException e) {
            e.printStackTrace();
            readError = true;
        }
    }

    public void ReadDataSetCSV(String filePath) throws IOException
    {
        BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(new File(filePath))));
        String line;

        boolean firstLine = true;

        int nc = 1;
        while((line = br.readLine()) != null)
        {
            List<String> cols = new ArrayList<>(Arrays.asList(line.split(",")));
            cols = cols.subList(1, cols.size());

            //Print("Extracted tokens: " + cols.size());

            names = new String[cols.size()];
            if(firstLine)
            {
                List<String> temp = new ArrayList<>();
                for(String col : cols) {
                    items.add(nc++);
                    temp.add(col.trim());
                }
                names = temp.toArray(new String[temp.size()]);
                Print("Name List: " + temp.toString());
                firstLine = false;
            } else {
                Print("Reading Transaction..");
                HashSet<Integer> transaction = new HashSet<>();
                int cc = 1;
                for(String col : cols)
                {
                    //either true or false
                    boolean bval = col.trim().equalsIgnoreCase("true");
                    if(bval)
                    {
                        transaction.add(cc);
                    }
                    cc++;
                }
                Print("Transaction: " + transaction.toString());
                transactions.add(transaction);
            }
        }
    }

    public Set<Set<Integer>> Setify(Set<Integer> set)
    {
        Set<Set<Integer>> N = new HashSet<>();
        Iterator<Integer> itr = set.iterator();
        while (itr.hasNext())
        {
            int item = itr.next();
            Set<Integer> itemSet = new HashSet<>();
            itemSet.add(item);
            N.add(itemSet);
        }
        return N;
    }

    public Set<Integer> Merge(Set<Integer> set1, Set<Integer> set2)
    {
        Set<Integer> merged = new HashSet<>();
        for(int item : set1)
            merged.add(item);
        for(int item : set2)
            merged.add(item);
        return merged;
    }

    public Set<Set<Integer>> JoinSelf(Set<Set<Integer>> set1, int length)
    {
        Set<Set<Integer>> joint = new HashSet<>();

        for(Set<Integer> i : set1)
        {
            for(Set<Integer> j : set1)
            {
                Set<Integer> merged = Merge(i, j);
                if(merged.size() == length)
                {
                    joint.add(merged);
                }
            }
        }

        return joint;
    }

    Map<Set<Integer>, Long> FreqSet;

    public long CountOccurance(Set<Integer> set)
    {
        //System.out.println("Checking for occurances of " + set.toString() + " in transactions");
        long count = 0;
        for(HashSet<Integer> transaction : transactions) {
            Iterator<Integer> itr = set.iterator();
            boolean subset = true;
            while (itr.hasNext()) {
                int item = itr.next();
                if(!transaction.contains(item)) {
                    //System.out.println(transaction.toString() + " does not contain " + item);
                    subset = false;
                }
                //System.out.println(transaction.toString() + " contains " + item);
            }
            if(subset) count++;
        }
        //System.out.println("Found " + count + " occurances");

        FreqSet.put(set, count);
        return count;
    }

    public Set<Set<Integer>> ReturnItemsWithMinSup(Set<Set<Integer>> itemSet) {
        Set<Set<Integer>> pruned = new HashSet<>();

        for(Set<Integer> item : itemSet)
        {
            long occ = CountOccurance(item);
            if(occ >= support)
            {
                pruned.add(item);
            }
        }
        return pruned;
    }

    public Set<Set<Integer>> PowerSet(Set<Integer> originalSet)
    {
        Set<Set<Integer>> powerSet = new HashSet<>();
        if(originalSet.isEmpty()) {
            powerSet.add(new HashSet<Integer>());
            return powerSet;
        }

        List<Integer> list = new ArrayList<>(originalSet);
        int head = list.get(0);
        Set<Integer> rest = new HashSet<>(list.subList(1, list.size()));
        for(Set<Integer> set: PowerSet(rest))
        {
            Set<Integer> tSet = new HashSet<>();
            tSet.add(head);
            tSet.addAll(set);
            powerSet.add(set);
            powerSet.add(tSet);
        }
        powerSet.remove(new HashSet<Integer>());
        return powerSet;
    }

    public void Execute(boolean ignoreTrivial)
    {
        if(readError)
        {
            Print("Dataset was improperly read.");
            return;
        }
        List<Set<Set<Integer>>> UKLK = new ArrayList<>();

        Set<Set<Integer>> currentL;
        Set<Set<Integer>> currentC;

        FreqSet = new HashMap<>();

        //L[0] <- add all items

        currentL = ReturnItemsWithMinSup(Setify(items));
        int k = 2;

        Print("L(k)" + currentL.toString());
        Print(FreqSet.toString());

        int safety = 100;
        int safetyCount = 0;
        while(currentL.size() != 0)
        {
            Print("================[K = " + k + "]====================");
            UKLK.add(currentL);

            //Print("Joining...");
            currentL = JoinSelf(currentL, k);
            //Print("Joined: (" + currentL.size() + ") " + currentL);
            //Print("");
            //Print("Pruning...");
            currentC = ReturnItemsWithMinSup(currentL);
            //Print("Joined with min_support: (" + currentL.size() + ") " + currentC);

            currentL = currentC;
            k++;

            //Print Diag

            Print("C[" + k + "] (" + currentL.size() + ")" + currentC.toString());
            Print("L[" + k + "] (" + currentL.size() + ")" + currentL.toString());
            Print("Freq " + FreqSet.toString());
            Print("UKLK " + UKLK.toString());

            if(++safetyCount == safety) return;
        }
        Print("");
        Print("Making association rules...");
        int lC = 0;
        for(Set<Set<Integer>> levelK : UKLK)
        {
            //Print("");
            //Print("");
            //Print("All sets in level " + ++lC + " " + levelK);
            for(Set<Integer> I : levelK)
            {
                //Print("");
                //Print("I: " + I);
                Set<Set<Integer>> powerSetOfI = PowerSet(I);
                //Print("Powerset of I: " + powerSetOfI);
                for(Set<Integer> S : powerSetOfI)
                {
                    if((double)FreqSet.get(I) / (double)FreqSet.get(S) >= confidence)
                    {
                        if(ignoreTrivial)
                            if(I.equals(S))
                                continue;
                        Print(S + " -> (" + I + " - " + S + ")");
                    }
                }
            }
        }
    }

    public void Print(String s)
    {
        System.out.println(s);
    }

}
