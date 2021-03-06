package se.simonevertsson.runner;

import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import se.simonevertsson.gpu.query.SpanningTree;

import java.util.ArrayList;

public class QueryGraph {

  public ArrayList<Node> nodes = new ArrayList<>();

  public ArrayList<Relationship> relationships = new ArrayList<>();

  private SpanningTree spanningTree;

  public AliasDictionary aliasDictionary = new AliasDictionary();

  public String toCypherQueryString() {
    StringBuilder builder = new StringBuilder();
    builder.append(toCypherQueryStringPrefix());
    builder.append(toCypherQueryStringSuffix());
    return builder.toString();
  }

  public String toCypherQueryStringPrefix() {
    StringBuilder builder = new StringBuilder();
    builder.append("MATCH ");
    boolean firstRelationship = true;
    for (Relationship relationship : this.relationships) {
      String startNodeAlias = this.aliasDictionary.getAliasForId((int) relationship.getStartNode().getId());
      String endNodeAlias = this.aliasDictionary.getAliasForId((int) relationship.getEndNode().getId());
      if (!firstRelationship) {
        builder.append(", ");
      } else {
        firstRelationship = false;
      }
      builder.append("(");
      builder.append(startNodeAlias);
      for (Label label : relationship.getStartNode().getLabels()) {
        builder.append(':');
        builder.append(label.name());
      }
      builder.append(")-");
      if (relationship.getType() != null) {
        builder.append("[:" + relationship.getType().name() + "]");
      }
      builder.append("->");
      builder.append("(");
      builder.append(endNodeAlias);
      for (Label label : relationship.getEndNode().getLabels()) {
        builder.append(':');
        builder.append(label.name());
      }
      builder.append(")");
    }
    return builder.toString();
  }

  public String toCypherQueryStringSuffix() {
    StringBuilder builder = new StringBuilder();
    builder.append(" RETURN ");
    boolean firstAlias = true;
    for (String alias : this.aliasDictionary.getAllAliases()) {
      if (!firstAlias) {
        builder.append(", ");
      } else {
        firstAlias = false;
      }
      builder.append(alias);
    }
    builder.append(";");
    return builder.toString();
  }

  @Override
  public boolean equals(Object obj) {

    return obj instanceof QueryGraph && (((QueryGraph) obj).toCypherQueryString().equals(this.toCypherQueryString()));
  }

  public SpanningTree getSpanningTree() {
    return spanningTree;
  }

  public void setSpanningTree(SpanningTree spanningTree) {
    this.spanningTree = spanningTree;
  }
}
