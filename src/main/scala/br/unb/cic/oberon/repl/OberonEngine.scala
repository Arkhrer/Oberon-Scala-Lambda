package br.unb.cic.oberon.repl

import br.unb.cic.oberon.ast.{AssignmentStmt, Constant, Expression, IntValue, IntegerType, REPLConstant, REPLExpression, REPLStatement, REPLUserTypeDeclaration, REPLVarDeclaration, StringValue, Undef, Value, VarAssignment, VarExpression, VariableDeclaration}
import br.unb.cic.oberon.interpreter.{EvalExpressionVisitor, Interpreter}
import br.unb.cic.oberon.parser.ScalaParser
import org.jline.console.{CmdDesc, CmdLine, ScriptEngine}
import org.jline.reader.Completer
import org.jline.reader.impl.completer.AggregateCompleter

import java.io.File
import java.nio.file.Path
import java.util
import java.util.Collections
import scala.jdk.CollectionConverters._

class OberonEngine extends ScriptEngine {
  object Format extends Enumeration {
    type Format = Value
    val JSON, OBERON, NONE = Value
  }

  val interpreter = new Interpreter
  val expressionEval = new EvalExpressionVisitor(interpreter)

  override def getEngineName: String = this.getClass.getSimpleName
  override def getExtensions: java.util.List[String] = Collections.singletonList("oberon")

  /*
   * TODO: implement script completer
   */
  override def getScriptCompleter: Completer = {
    new AggregateCompleter()
  }

  override def hasVariable(name: String): Boolean = interpreter.env.lookup(name).isDefined

  override def put(name: String, value: Object): Unit = {
    // println(f"put call ($name = $value)")
    val valueExpr = value.asInstanceOf[Any] match {
      case i: Int => IntValue(i)
      case s: String => StringValue(s)
      case e: Exception => StringValue(e.getMessage)
      case e: Expression => e
      //case _: BoxedUnit => Undef()
      case default => {
        println(f"Invalid .put call $name = ${value.toString} (${default.getClass.getSimpleName})")
        IntValue(0)
      }
    }

    Constant(name, valueExpr).accept(interpreter)
  }

  override def get(name: String): Object = {
    val variable = interpreter.env.lookup(name)
    if (variable.isDefined) variable.get.accept(expressionEval) else null
  }

  override def find(name: String): util.Map[String, Object] = {
    if (name == null) {
      val allVariables = interpreter.env.allVariables()
      (allVariables zip allVariables.map(v => get(v))).toMap.asJava
    } else {
      val filteredVariables = internalFind(name)
      (filteredVariables zip filteredVariables.map(v => get(v))).toMap.asJava
    }
  }

  override def del(vars: String*): Unit = {
    if (vars == null) {
      return
    }
    vars.foreach(del)
  }

  def del(variable: String): Unit = {
    if (variable == null) {
      return
    }
    if (hasVariable(variable)) {
      interpreter.env.delVariable(variable)
    }
  }

  /*
   * TODO: implement toJson
   */
  override def toJson(obj: Object): String = {
    println("toJson call")
    "TODO: toJson"
  }

  /*
   * TODO: implement toString
   */
  override def toString(obj: Object): String = {
    println("toString call")
    "TODO: toString"
  }

  /*
   * TODO: implement toMap
   */
  override def toMap(obj: Object): util.Map[String, Object] = {
    println("toMap call")
    println(obj)
    println(obj.getClass.getSimpleName)
    null
  }

  override def getSerializationFormats: util.List[String] = List(Format.JSON.toString, Format.NONE.toString).asJava
  override def getDeserializationFormats: util.List[String] = List(Format.JSON.toString, Format.OBERON.toString, Format.NONE.toString).asJava

  /*
   * TODO: implement deserialize
   */
  override def deserialize(value: String, formatStr: String): Object = {
    val out = value.asInstanceOf[Object];
    val format = if (formatStr != null && formatStr.nonEmpty) Format.withName(formatStr.toUpperCase) else null
    if (format == Format.NONE) {
      // do nothing
    } else if (format == Format.JSON) {
      // TODO: JSON deserialization
      return new util.HashMap[String, Object]()
    } else if (format == Format.OBERON) {
      // TODO: Oberon deserialization
    } else {
      // TODO: Undefined deserialization
    }
    out
  }

  override def persist(file: Path, obj: Object): Unit = persist(file, obj, getSerializationFormats().get(0));

  /*
   * TODO: implement persist
   * https://github.com/jline/jline3/blob/master/console/src/main/java/org/jline/console/ConsoleEngine.java#L125
   */
  override def persist(file: Path, obj: Object, format: String): Unit = {

  }

  /*
   * TODO: execute file with arguments (replace $1, $2, ... with parameters values)
   */
  override def execute(script: File, args: Array[Object]): Object = {
    null
  }

  /*
   * TODO: improve execute statement
   */
  override def execute(statement: String): Any = {
    try {
      val command = ScalaParser.parserREPL(statement)
      command match {
        case v: REPLVarDeclaration =>
          v.declarations.foreach(variable => variable.accept(interpreter))
        case c: REPLConstant =>
          c.constants.accept(interpreter)
          val result = c.constants.exp.accept(expressionEval)
          result match {
            case v: Value => return v.value
            case _: Undef => return
          }
          return result
        case u: REPLUserTypeDeclaration =>
          u.userTypes.accept(interpreter)
        case s: REPLStatement =>
          s.stmt.accept(interpreter)
        case e: REPLExpression =>
          val result = e.exp.accept(expressionEval)
          result match {
            case v: Value => return v.value
            case _: Undef => return
          }
          return result
      }
    }
    catch {
      case v: ClassCastException => println("This is an invalid operation: " + v.getMessage)
      case e: NoSuchElementException => println("A variable is not defined " + e.getMessage)
      case n: NullPointerException => println("This is an invalid operation")
      case d: Throwable => println(d)
    }
    null
  }

  override def execute(closure: Object, args: Object*): Object = ???

  private def internalFind(variable: String): List[String] = interpreter.env.allVariables().filter(v => v.matches(variable)).toList

  def scriptDescription(line: CmdLine): CmdDesc = {
    val out = new CmdDesc
    // TODO: Script description = out.setMainDesc()
    out
  }//new Inspector(this).scriptDescription(line)
}
