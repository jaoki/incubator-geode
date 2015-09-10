/*=========================================================================
 * Copyright (c) 2005-2014 Pivotal Software, Inc. All Rights Reserved.
 * This product is protected by U.S. and international copyright
 * and intellectual property laws. Pivotal products are covered by
 * more patents listed at http://www.pivotal.io/patents.
 *========================================================================
 */
package com.gemstone.gemfire.cache.query.internal.parse;

import antlr.*;
import com.gemstone.gemfire.cache.query.internal.QCompiler;
/**
 * 
 * @author Yogesh Mahajan
 *
 */
public class ASTGroupBy extends GemFireAST {
  private static final long serialVersionUID = 2262777181888775078L;

  public ASTGroupBy(Token t) {
    super(t);
  }
  
  public ASTGroupBy() { }


  @Override
    public void compile(QCompiler compiler)
    {
        super.compile(compiler);
        compiler.compileGroupByClause(this.getNumberOfChildren());
    }

}

