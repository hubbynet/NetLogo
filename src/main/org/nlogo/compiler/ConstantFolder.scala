// (C) Uri Wilensky. https://github.com/NetLogo/NetLogo

package org.nlogo.compiler

// Performs constant folding (http://en.wikipedia.org/wiki/Constant_folding)

// What if a runtime error occurs when we try to fold?  Currently, we
// report it to the user at compile-time.  But we could wait until
// runtime to deal with it.  Perhaps users will find it disconcerting
// that some of their code is being "run" at compile-time, and errors
// pop up.  Waiting evaluation would also allow us to compile division
// by zero, etc.  The problem with waiting is that right now the
// bytecode generator doesn't gracefully handle bad inputs.  Since
// impossible things like "5 - true" can make the bytecode generator
// choke, for now it's better to catch these errors here, rather than
// hand them on to the bytecode generator, where bad things will
// happen.  ~Forrest (10/13/2006)
// I think compile time reporting is definitely good. - ST 2/12/09

import org.nlogo.compiler.CompilerExceptionThrowers._

import org.nlogo.api.CompilerException
import org.nlogo.api.LogoException
import org.nlogo.nvm.Pure

private class ConstantFolder extends DefaultAstVisitor {
  override def visitReporterApp(app:ReporterApp) {
    super.visitReporterApp(app)
    if(app.reporter.isInstanceOf[Pure] && !app.args.isEmpty && app.args.forall(isConstant)) {
      val newReporter = ConstantParser.makeConstantReporter(applyReporter(app))
      newReporter.storedSourceStartPosition = app.reporter.getSourceStartPosition
      newReporter.storedSourceEndPosition = app.reporter.getSourceEndPosition
      app.reporter = newReporter
      app.clearArgs
    }
  }
  private def isConstant(e:Expression) =
    e match {
      case app:ReporterApp => app.reporter.isInstanceOf[Pure] && app.args.isEmpty
      case _ => false
    }
  private def applyReporter(app:ReporterApp):Object = {
    val r = app.reporter
    app.accept(new ArgumentStuffer) // fill args array
    r.init(null)  // copy args array to arg0, arg1, etc.
    try { r.report(null) }
    catch { case ex:LogoException => exception(CompilerException.RuntimeErrorAtCompileTimePrefix + ex.getMessage,app) }
  }
}
