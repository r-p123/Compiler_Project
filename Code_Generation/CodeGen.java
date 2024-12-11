import java.util.ArrayList;
import java.util.HashMap;
import java.util.Stack;


class CodeGen implements Visitor{

    HashMap<String, ArrayList<String>> M = new HashMap<>();

    int fpCount = -12;
    String currLabel = "";
    Stack<String> labels = new Stack<>();


    CodeGen(){
    }

    public void printMips(HashMap<String, ArrayList<String>> M){

        for(String m : M.keySet()){

            for(String s : M.get(m)){
                System.out.println(s);
            }

        }
    }

    String traverse(ASTNode node){
        SymbolTable.stack.clear();

        System.out.println("\t# standard Decaf preamble\n");
        System.out.println("\t.text");
        System.out.println("\t.align 2");
        System.out.println("\t.globl main");
        node.accept(this);

        printMips(M);

        return null;
    }


    @Override
    public String visitProgramNode(ProgramNode programNode) {

        ArrayList<ASTNode> A = programNode.decls;

        for (int i = 0; i < A.size(); i++){
            SymbolTable.reset();
            fpCount = -12;
            A.get(i).accept(this);

            //printMips(M);

        }
        return null;
    }

    @Override
    public String visitVariableNode(VariableNode variableNode) {
        if(SymbolTable.stack.isEmpty()){
            HashMap<String, ASTNode> scope1 = new HashMap<>();
            scope1.put(variableNode.var.name, variableNode);
            SymbolTable.stack.push(scope1);
        }else{
            HashMap<String, ASTNode> scope1 = SymbolTable.stack.pop();
            scope1.put(variableNode.var.name, variableNode);
            SymbolTable.stack.push(scope1);
        }
        return null;
    }

    @Override
    public String visitFunctionNode(FunctionNode functionNode) {

        //prologue
        labels.push(functionNode.Identifier);
        currLabel = functionNode.Identifier;

        M.put(labels.peek(), new ArrayList<String>());


        M.get(labels.peek()).add(String.format("%s:\n", functionNode.Identifier));

        M.get(labels.peek()).add("\tsubu $sp, $sp, 8");
        M.get(labels.peek()).add("\tsw $fp, 8($sp)");
        M.get(labels.peek()).add("\tsw $ra, 4($sp)");
        M.get(labels.peek()).add("\taddiu $fp, $sp, 8");
        M.get(labels.peek()).add("*");

        Register.reset();


        if(functionNode.formals.size() > 0){
            for(int i = 0; i<functionNode.formals.size(); i++){
                Register r = Register.Next();
                M.get(labels.peek()).add(String.format("\tlw $t%d, %d($fp)", r.reg, (i+1)*4));
                r.setWhereStored((i+1)*4);
                functionNode.formals.get(i).reg = r;
                if(SymbolTable.stack.isEmpty()){
                    HashMap<String, ASTNode> scope1 = new HashMap<>();
                    scope1.put(functionNode.formals.get(i).var.name, functionNode.formals.get(i));
                    SymbolTable.stack.push(scope1);
                }else{
                    HashMap<String, ASTNode> scope1 = SymbolTable.stack.pop();
                    scope1.put(functionNode.formals.get(i).var.name, functionNode.formals.get(i));
                    SymbolTable.stack.push(scope1);
                }

            }
        }

        functionNode.stmtBlock.accept(this);
        M.get(labels.peek()).add("\tmove $sp, $fp");
        M.get(labels.peek()).add("\tlw $ra, -4($fp)");
        M.get(labels.peek()).add("\tlw $fp, 0($fp)");
        M.get(labels.peek()).add("\tjr $ra");

        Register r = Register.Next();

        for(int i = 0; i < M.get(labels.peek()).size(); i++){
            if(M.get(labels.peek()).get(i).equals("*")){
                M.get(labels.peek()).set(i, String.format("\tsubu $sp, $sp, %d", 4*4*r.reg));
            }
        }

        //printMips(mips);

        return null;
    }




    @Override
    public String visitStmtBlockNode(StmtBlockNode stmtBlockNode){

        //just adds to symbol table
        for(int i = 0; i < stmtBlockNode.varDecls.size(); i++){
            stmtBlockNode.varDecls.get(i).accept(this);
        }

        for(int i = 0; i < stmtBlockNode.stmts.size(); i++){
            stmtBlockNode.stmts.get(i).accept(this);
        }

        return null;
    }


    @Override
    public String visitIfStmtNode(IfStmtNode ifStmtNode) {

        ifStmtNode.expr.accept(this);
        Register r = ifStmtNode.expr.reg;

        Label elseLabel = Label.Next();
        Label endLabel = Label.Next();

        M.get(labels.peek()).add(String.format("beqz $t%d %s", r.reg, elseLabel.labelName));


        if(ifStmtNode.stmt1 != null) {
            ifStmtNode.stmt1.accept(this);
            M.get(labels.peek()).add(String.format("b %s", endLabel.labelName));
        }


        M.get(labels.peek()).add(String.format("%s:", elseLabel.labelName));
        if(ifStmtNode.stmt2 != null) {

            ifStmtNode.stmt2.accept(this);
            M.get(labels.peek()).add(String.format("j %s", endLabel.labelName));

        }
        M.get(labels.peek()).add(String.format("%s:", endLabel.labelName));


        return null;

    }

    @Override
    public
    String visitListOfExpr(ListOfExpr listOfExpr) {

        ArrayList<ExprNode> A = listOfExpr.exprList;

        for(int i = 0; i < A.size(); i++){
            A.get(i).accept(this);
        }
        return null;
    }

    @Override
    public String visitWhileStmtNode(WhileStmtNode whileStmtNode) {
        Label startLabel = Label.Next();
        M.get(labels.peek()).add(String.format("%s:", startLabel.labelName));

        whileStmtNode.exprNode.accept(this);
        Register r = Register.Next();

        Label endLabel = Label.Next();
        M.get(labels.peek()).add(String.format("beqz $t%d %s", r.reg, endLabel.labelName));

        whileStmtNode.stmt.accept(this);

        M.get(labels.peek()).add(String.format("b %s", startLabel.labelName));


        M.get(labels.peek()).add(String.format("%s:", endLabel.labelName));

        return null;

    }

    @Override
    public String visitPrintStmtNode(PrintStmtNode printStmtNode) {


        for(int i = 0; i < printStmtNode.exprNodes.size(); i++){

            printStmtNode.exprNodes.get(i).accept(this);
            //printStmtNode.exprNodes.get(i).typeCheck();

            fpCount-=4;
        }

        for(int i = printStmtNode.exprNodes.size()-1; i >=0 ; i--){
            M.get(labels.peek()).add("");
            M.get(labels.peek()).add("\tsubu $sp, $sp, 4");

            M.get(labels.peek()).add(String.format("\tlw $t%d, %s", 0, printStmtNode.exprNodes.get(i).reg.getWhereStored()));
            M.get(labels.peek()).add(String.format("\tsw $t%d, 4($sp)", 0));
        }

        Register r = Register.Next();

        if(printStmtNode.exprNodes.get(0).type.equals("int")){
            M.get(labels.peek()).add(String.format("\tjal _PrintInt"));
        }else{
            M.get(labels.peek()).add(String.format("\tjal _PrintString"));
        }



        M.get(labels.peek()).add(String.format(""));


        M.get(labels.peek()).add(String.format("\tadd $sp, $sp, %d", 4*printStmtNode.exprNodes.size()));


        return null;
    }

    @Override
    public String visitBreakStmtNode(BreakStmtNode breakStmtNode) {

        return null;
    }

    @Override
    public String visitReturnStmtNode(ReturnStmtNode returnStmtNode) {

        if(returnStmtNode.getExprNode() != null){

            returnStmtNode.expr.typeCheck();
            returnStmtNode.expr.accept(this);

        }
        Register res = Register.Next();

        M.get(labels.peek()).add(String.format("\tlw $t%d, %s", res.reg, returnStmtNode.expr.reg.getWhereStored()));
        M.get(labels.peek()).add(String.format("\tmove $v0, $t%d", res.reg));

        M.get(labels.peek()).add("\tmove $sp, $fp");
        M.get(labels.peek()).add("\tlw $ra, -4($fp)");
        M.get(labels.peek()).add("\tlw $fp, 0($fp)");
        M.get(labels.peek()).add("\tjr $ra");

        return null;

    }

    @Override
    public String visitForStmtNode(ForStmtNode forStmtNode) {

        if(forStmtNode.expr1 != null){
            forStmtNode.expr1.accept(this);
        }

        Label condLabel = Label.Next();
        forStmtNode.expr2.accept(this);
        M.get(labels.peek()).add(String.format("%s:", condLabel.labelName));

        Register rExpr = forStmtNode.expr2.reg;
        Label endLabel = Label.Next();
        M.get(labels.peek()).add(String.format("beqz $t%d %s", rExpr.reg, endLabel.labelName));


        if(forStmtNode.expr3 != null){
            forStmtNode.expr3.accept(this);
        }
        forStmtNode.stmtNode.accept(this);

        M.get(labels.peek()).add(String.format("%s:", endLabel.labelName));


        return null;

    }

    @Override
    public
    String visitActualNode(ActualNode actualNode) {

        for(int i = 0; i< actualNode.exprList.size(); i++){
            actualNode.exprList.get(i).accept(this);
        }

        return null;
    }

    @Override
    public
    String visitCallNode(CallNode callNode) {

        if(SymbolTable.stack.isEmpty()){
            HashMap<String, ASTNode> scope1 = new HashMap<>();
            scope1.put(callNode.ident.name, callNode.actualNode);
            SymbolTable.stack.push(scope1);
        }else{
            HashMap<String, ASTNode> scope1 = SymbolTable.stack.pop();
            scope1.put(callNode.ident.name, callNode.actualNode);
            SymbolTable.stack.push(scope1);
        }

        //load and store params
        for(int i = 0; i < callNode.actualNode.exprList.size(); i++){
            callNode.actualNode.exprList.get(i).accept(this);
            fpCount-=4;
        }


        //push params on to stack
        for(int i = callNode.actualNode.exprList.size()-1; i >=0 ; i--){

            M.get(labels.peek()).add("\tsubu $sp, $sp, 4");
            M.get(labels.peek()).add(String.format("\tlw $t%d, %s", 0, callNode.actualNode.exprList.get(i).reg.getWhereStored()));
            M.get(labels.peek()).add(String.format("\tsw $t%d, 4($sp)", 0));
        }

        Register r = Register.Next();
        //r.setSaved(fpCount);
        M.get(labels.peek()).add("");
        M.get(labels.peek()).add(String.format("\tjal %s", callNode.ident.name));
        M.get(labels.peek()).add(String.format("\tmove $t%d, $v0", r.reg));
        M.get(labels.peek()).add(String.format("\tsw $t%d, %d($fp)", r.reg, fpCount));
        r.setWhereStored(fpCount);
        callNode.reg = r;

        M.get(labels.peek()).add(String.format(""));


        M.get(labels.peek()).add(String.format("\tadd $sp, $sp, %d", 4*callNode.actualNode.exprList.size()));
        return null;
    }

    @Override
    public String visitExprNode(ExprNode exprNode){

        if(exprNode.operator == null) {
            if(exprNode.left instanceof Ident){
                exprNode.reg = SymbolTable.lookUpType(((Ident) exprNode.left).name).reg;
            }else {
                exprNode.left.accept(this);
                exprNode.reg = exprNode.left.reg;
            }

        } else if(exprNode.operator.equals("+") || exprNode.operator.equals("*") || exprNode.operator.equals("/") || exprNode.operator.equals("-") || exprNode.operator.equals("%")){
            exprNode.left.accept(this);
            Register x = exprNode.left.reg;

            exprNode.right.accept(this);
            Register y = exprNode.right.reg;

            Register result = Register.Next();
            String op = "";

            switch(exprNode.operator){
                case "+":
                    op = "add"; break;
                case "-":
                    op = "sub"; break;
                case "*":
                    op = "mul"; break;
                case "/":
                    op = "div"; break;
                case "%":
                    op = "rem"; break;

            }
            result.setWhereStored(fpCount);
            Register reg1 = Register.Next();
            Register reg2 = Register.Next();

            M.get(labels.peek()).add(String.format("\tlw $t%d, %s", reg1.reg, x.getWhereStored()));
            M.get(labels.peek()).add(String.format("\tlw $t%d, %s", reg2.reg, y.getWhereStored()));

            M.get(labels.peek()).add(String.format("\t%s $t%d, $t%d, $t%d", op, result.reg, reg1.reg, reg2.reg));
            M.get(labels.peek()).add(String.format("\tsw $t%d, %s", result.reg, result.whereStored));

            exprNode.reg = result;
            fpCount-=4;

        }else if(exprNode.operator.equals("=")){

            exprNode.right.accept(this);
//            Register r1 = exprNode.right.reg;

            if(exprNode.left instanceof Ident){
                Ident ident = (Ident) exprNode.left;
                Register r2 = Register.Next();

                SymbolTable.lookUpType(ident.name).reg = exprNode.right.reg;

                M.get(labels.peek()).add(String.format("\tlw $t%d, %s", r2.reg, exprNode.right.reg.whereStored));
                M.get(labels.peek()).add(String.format("\tsw $t%d, %s", r2.reg, exprNode.right.reg.whereStored));

            }
            exprNode.reg = exprNode.right.reg;

            M.get(labels.peek()).add("");
            fpCount -= 4;

        }else if(exprNode.operator.equals("<") || exprNode.operator.equals("<=") || exprNode.operator.equals(">") || exprNode.operator.equals(">=") || exprNode.operator.equals("==")){
            exprNode.left.accept(this);
            Register x = exprNode.left.reg;

            exprNode.right.accept(this);
            Register y = exprNode.right.reg;

            Register result = Register.Next();
            String op = "";

            switch(exprNode.operator){
                case "<":
                    op = "slt"; break;
                case "<=":
                    op = "sle"; break;
                case ">":
                    op = "sgt"; break;
                case ">=":
                    op = "sge"; break;
                case "==":
                    op = "seq"; break;
            }
            result.setWhereStored(fpCount);
            M.get(labels.peek()).add(String.format("\t%s $t%d, $t%d, $t%d", op, result.reg, x.reg, y.reg));
            M.get(labels.peek()).add(String.format("\tsw $t%d, %s", result.reg, result.whereStored));

            exprNode.reg = result;
            fpCount-=4;
        }

        return null;
    }



    @Override
    public String visitIdentNode(Ident ident){

        Register r1 = Register.Next();

        Register r2 = SymbolTable.lookUpType(ident.name).reg;

        M.get(labels.peek()).add(String.format("\tlw $t%d, %s", r2.reg, r2.whereStored));

        return null;
    }

    @Override
    public String visitIntNode(IntNode intNode){

        Register r = Register.Next();
        //r.setSaved(fpCount);
        intNode.reg = r;

        M.get(labels.peek()).add(String.format("\tli $t%d, %d", r.reg, Integer.parseInt(intNode.name)));
        M.get(labels.peek()).add(String.format("\tsw $t%d, %d($fp)", r.reg, fpCount));
        r.setWhereStored(fpCount);
        fpCount-=4;

        Register.clearReg(r.reg);
        M.get(labels.peek()).add("");
        return null;
    }

    @Override
    public String visitDoubleNode(DoubleNode doubleNode){

        return null;
    }

    @Override
    public String visitBoolNode(BoolNode boolNode){

        return null;
    }

    @Override
    public String visitStringNode(StringNode stringNode){
        M.get(labels.peek()).add("\t# make string");
        M.get(labels.peek()).add(String.format("\t.data"));
        M.get(labels.peek()).add(String.format("\t_string1: .asciiz %s", stringNode.name));
        M.get(labels.peek()).add("\t.text");

        Register r = Register.Next();
        stringNode.reg = r;

        M.get(labels.peek()).add(String.format("\tla $t%d, _string1", r.reg));
        M.get(labels.peek()).add(String.format("\tsw $t%d, %d($fp)", r.reg, fpCount));
        r.setWhereStored(fpCount);
        Register.clearReg(r.reg);

        fpCount-=4;

        M.get(labels.peek()).add("");

        return null;
    }

    @Override
    public String visitReadIntNode(ReadIntNode readIntNode){

        return null;
    }

    @Override
    public String visitReadLineNode(ReadLineNode readLineNode){

        return null;
    }

    @Override
    public String visitNullNode(NullNode nullNode){

        return null;
    }




}