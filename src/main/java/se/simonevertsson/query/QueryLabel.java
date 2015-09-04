package se.simonevertsson.query;

import org.neo4j.graphdb.Label;

/**
 * Created by simon.evertsson on 2015-05-18.
 */
public class QueryLabel implements Label {
    private final String name;

    public QueryLabel(String name) {
        this.name = name;
    }

    public String name() {
        return this.name;
    }


    @Override
    public boolean equals(Object that) {
        return (that instanceof Label) && this.name().equals(((Label) that).name());
    }


    @Override
    public int hashCode() {
        return this.name().hashCode();
    }
}
