package org.netbeans.modules.cnd.antlr;

/* ANTLR Translator Generator
 * Project led by Terence Parr at http://www.cs.usfca.edu
 * Software rights: http://www.antlr.org/license.html
 */

abstract class BlockWithImpliedExitPath extends AlternativeBlock {
    protected int exitLookaheadDepth;	// lookahead needed to handle optional path
    /** lookahead to bypass block; set
     * by deterministic().  1..k of Lookahead
     */
    protected Lookahead[] exitCache = new Lookahead[grammar.maxk + 1];

    public BlockWithImpliedExitPath(Grammar g) {
        super(g);
    }

    public BlockWithImpliedExitPath(Grammar g, Token start) {
        super(g, start, false);
    }
}
