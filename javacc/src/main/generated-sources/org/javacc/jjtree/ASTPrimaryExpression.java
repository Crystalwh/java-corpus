/* Generated By:JJTree: Do not edit this line. ASTPrimaryExpression.java Version 4.1 */
/* JavaCCOptions:MULTI=true,NODE_USES_PARSER=false,VISITOR=true,TRACK_TOKENS=false,NODE_PREFIX=AST,NODE_EXTENDS=,NODE_FACTORY= */
package org.javacc.jjtree;

public class ASTPrimaryExpression extends JJTreeNode{
  public ASTPrimaryExpression(int id) {
    super(id);
  }

  public ASTPrimaryExpression(JJTreeParser p, int id) {
    super(p, id);
  }


  /** Accept the visitor. **/
  public Object jjtAccept(JJTreeParserVisitor visitor, Object data) {
    return visitor.visit(this, data);
  }
}
/* JavaCC - OriginalChecksum=5f967e9bf70d30209a140d5ae9e9e3b2 (do not edit this line) */
