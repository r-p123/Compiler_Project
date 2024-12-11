import java.util.ArrayList;

class AstPrinter implements Visitor{

    StringBuilder tabs = new StringBuilder();
    StringBuilder ans = new StringBuilder();

    AstPrinter(){

    }

    String print(ASTNode node){
        return node.accept(this);
    }

    private void addTab(){
        tabs.append("\t");
    }
    private void remTab(){
        tabs.deleteCharAt(tabs.length()-1);
    }


    @Override
    public String visitProgramNode(ProgramNode programNode) {

        ArrayList<ASTNode> A = programNode.decls;
        ans.append("Program:\n");
        for (int i = 0; i < A.size(); i++){

                addTab();
                ans.append(A.get(i).accept(this));
                remTab();

        }
        return ans.toString();
    }

    @Override
    public String visitVariableNode(VariableNode variableNode) {

        StringBuilder str = new StringBuilder();

        str.append(tabs.toString() + "VarDecl:\n");
        str.append(tabs.toString() + "\tType: " + variableNode.type + "\n");
        str.append(tabs.toString() + "\tIdentifier: " + variableNode.var + "\n");
        return str.toString();
    }

    @Override
    public String visitFunctionNode(FunctionNode functionNode) {

        StringBuilder str = new StringBuilder();

        str.append(tabs.toString() + "FnDecl:\n");
        str.append(tabs.toString() + "\t" + "(return type) Type: " + functionNode.returnType + "\n");
        str.append(tabs.toString() + "\tIdentifier: " + functionNode.Identifier + "\n");

        if(functionNode.formals.size() > 0){
            for(int i = 0; i<functionNode.formals.size(); i++){
                addTab();
                str.append(tabs.toString() + "(formals) \n");
                str.append(functionNode.formals.get(i).accept(this) + "\n");
                remTab();
            }
        }

        str.append(tabs.toString() + "\t(body): StmtBlock\n");
        addTab();
        str.append(functionNode.stmtBlock.accept(this));
        remTab();

        return str.toString();
    }

    @Override
    public String visitStmtBlockNode(StmtBlockNode stmtBlockNode){
        StringBuilder str = new StringBuilder();

        addTab();
        for(int i = 0; i < stmtBlockNode.varDecls.size(); i++){
            str.append(stmtBlockNode.varDecls.get(i).accept(this));
        }

        for(int i = 0; i < stmtBlockNode.stmts.size(); i++){
            str.append(stmtBlockNode.stmts.get(i).accept(this));
        }

        remTab();
        return str.toString();
    }


    @Override
    public String visitIfStmtNode(IfStmtNode ifStmtNode) {

        StringBuilder str = new StringBuilder();

        str.append(tabs.toString() + "IfStmt:\n");
        addTab();
        str.append(tabs.toString() + "(test)  " + stripWhiteSpace(ifStmtNode.expr.accept(this)) + "\n");

        if(ifStmtNode.stmt1 instanceof ListOfExpr){

        }
        if(ifStmtNode.stmt1 != null) {
            str.append(tabs.toString() + "(then): " + stripWhiteSpace(ifStmtNode.stmt1.accept(this)) + "\n");
        }

        if(ifStmtNode.stmt2 != null) {
            str.append(tabs.toString() + "(else): " + stripWhiteSpace(ifStmtNode.stmt2.accept(this)) + "\n");
        }

        remTab();

        str.append("\n");
        return str.toString();
    }

    @Override
    public String visitStmtNode(StmtNode stmtNode){


        return null;
    }

    @Override
    public
    String visitListOfExpr(ListOfExpr listOfExpr) {

        StringBuilder str = new StringBuilder();
        ArrayList<ExprNode> A = listOfExpr.exprList;

        for(int i = 0; i < A.size(); i++){
            str.append(A.get(i).accept(this));
        }
        return str.toString();
    }

    @Override
    public String visitWhileStmtNode(WhileStmtNode whileStmtNode) {

        StringBuilder str = new StringBuilder();

        str.append(tabs.toString() + "WhileStmt:\n");

        addTab();
        str.append(tabs.toString() + "(test)  " + stripWhiteSpace(whileStmtNode.exprNode.accept(this)) + "\n");
        str.append(tabs.toString() + "(body) StmtBlock:\n" + whileStmtNode.stmt.accept(this) + "\n");
        remTab();

        return str.toString();

    }

    @Override
    public
    String visitPrintStmtNode(PrintStmtNode printStmtNode) {
        StringBuilder str = new StringBuilder();

        str.append(tabs.toString() + "PrintStmt:\n");

        addTab();
        for(int i = 0; i<printStmtNode.exprNodes.size(); i++){
            str.append(tabs.toString() + "(args)  " + stripWhiteSpace(printStmtNode.exprNodes.get(i).accept(this)) + "\n");
        }
        remTab();

        return str.toString();
    }

    @Override
    public String visitBreakStmtNode(BreakStmtNode breakStmtNode) {
        StringBuilder str = new StringBuilder();
        str.append(tabs.toString() + "BreakStmt");

        return str.toString();
    }

    @Override
    public
    String visitReturnStmtNode(ReturnStmtNode returnStmtNode) {

        StringBuilder str = new StringBuilder();

        str.append(tabs.toString() + "ReturnStmt:\n");

        addTab();
        if(returnStmtNode.getExprNode() != null){
            str.append(tabs.toString() + stripWhiteSpace(returnStmtNode.expr.accept(this)) + "\n");

        }else{
            str.append(tabs.toString() + "empty");
        }
        remTab();

        return str.toString();

    }

    @Override
    public
    String visitForStmtNode(ForStmtNode forStmtNode) {

        StringBuilder str = new StringBuilder();

        str.append(tabs.toString() + "ForStmt:\n");

        addTab();

        if(forStmtNode.expr1 != null){
            str.append(tabs.toString() + "(init)" + stripWhiteSpace(forStmtNode.expr1.accept(this)) + "\n");
        }else{
            str.append(tabs.toString() + "(init) Empty: \n");
        }


        str.append(tabs.toString() + "(test)" + stripWhiteSpace(forStmtNode.expr2.accept(this)) + "\n");

        if(forStmtNode.expr3 != null){
            str.append(tabs.toString() + "(step)" + stripWhiteSpace(forStmtNode.expr3.accept(this)) + "\n");
        }else{
            str.append(tabs.toString() + "(step) Empty: \n");
        }

        str.append(tabs.toString() + "(body) StmtBlock:\n" + forStmtNode.stmtNode.accept(this) + "\n");

        remTab();

        return str.toString();

    }

    @Override
    public
    String visitActualNode(ActualNode actualNode) {

        StringBuilder str = new StringBuilder();
        addTab();

        for(int i = 0; i< actualNode.exprList.size(); i++){
            str.append(tabs.toString() + "(actuals) " + stripWhiteSpace(actualNode.exprList.get(i).accept(this)) + "\n");
        }

        remTab();
        return str.toString();
    }

    @Override
    public
    String visitCallNode(CallNode callNode) {
        StringBuilder str = new StringBuilder();

        str.append(tabs.toString() + "Call:\n");

        addTab();
        str.append(tabs.toString() + "Identifier: " + callNode.ident.name + "\n");

        for(int i = 0; i< callNode.actualNode.exprList.size(); i++){
            str.append(tabs.toString() + "(actuals) " + stripWhiteSpace(callNode.actualNode.exprList.get(i).accept(this)) + "\n");
        }

        remTab();
        return str.toString();
    }

    @Override
    public String visitExprNode(ExprNode exprNode){

        StringBuilder str = new StringBuilder();


        String typeOfExpr = "empty";

        if( matches(exprNode.operator, "*", "/", "%", "+", "-")){
                typeOfExpr = "ArithmeticExpr";
        }else if(matches(exprNode.operator,"<", "<=", ">", ">=")){
                typeOfExpr = "RelationalExpr";
        }else if(matches(exprNode.operator, "==", "!=")){
                typeOfExpr = "EqualityExpr";
        }else if(matches(exprNode.operator,"||", "&&")){
                typeOfExpr = "LogicalExpr";
        }else if(matches(exprNode.operator, "=")){
            typeOfExpr = "AssignExpr";
        }


        str.append(tabs.toString() + typeOfExpr + "\n" );

        addTab();
        str.append(exprNode.left.accept(this));
        str.append(tabs.toString() + "Operator: " + exprNode.operator  + "\n");
        str.append(exprNode.right.accept(this));
        remTab();


        return str.toString();
    }

    @Override
    public String visitIdentNode(Ident ident){

        StringBuilder str = new StringBuilder();
        str.append(tabs.toString() + "FieldAccess:\n");
        if(ident.operator.length() > 0){
            str.append(tabs.toString() + "Operator: " + ident.operator +  "\n");
        }
        str.append(tabs.toString() + "\tIdentifier: " + ident.name + "\n");

        return str.toString();
    }

    @Override
    public String visitIntNode(IntNode intNode){

        StringBuilder str = new StringBuilder();
        if(intNode.operator.length() > 0){
            str.append(tabs.toString() + "Operator: " + intNode.operator +  "\n");
        }
        str.append(tabs.toString() + "\tIntConstant: " + intNode.name + "\n");

        return str.toString();
    }

    @Override
    public String visitDoubleNode(DoubleNode doubleNode){

        StringBuilder str = new StringBuilder();
        if(doubleNode.operator.length() > 0){
            str.append(tabs.toString() + "Operator: " + doubleNode.operator +  "\n");
        }
        str.append(tabs.toString() + "\tDoubleConstant: " + doubleNode.name + "\n");

        return str.toString();
    }

    @Override
    public String visitBoolNode(BoolNode boolNode){

        StringBuilder str = new StringBuilder();
        if(boolNode.operator.length() > 0){
            str.append(tabs.toString() + "Operator: " + boolNode.operator +  "\n");
        }
        str.append(tabs.toString() + "\tBoolConstant: " + boolNode.name + "\n");

        return str.toString();
    }

    @Override
    public String visitStringNode(StringNode stringNode){

        StringBuilder str = new StringBuilder();
        if(stringNode.operator.length() > 0){
            str.append(tabs.toString() + "Operator: " + stringNode.operator +  "\n");
        }
        str.append(tabs.toString() + "\tStringConstant: " + stringNode.name + "\n");

        return str.toString();
    }

    @Override
    public String visitReadIntNode(ReadIntNode readIntNode){

        StringBuilder str = new StringBuilder();
        if(readIntNode.operator.length() > 0){
            str.append(tabs.toString() + "Operator: " + readIntNode.operator +  "\n");
        }
        str.append(tabs.toString() + "\tReadIntegerExpr\n" );

        return str.toString();
    }

    @Override
    public String visitReadLineNode(ReadLineNode readLineNode){

        StringBuilder str = new StringBuilder();
        if(readLineNode.operator.length() > 0){
            str.append(tabs.toString() + "Operator: " + readLineNode.operator +  "\n");
        }
        str.append(tabs.toString() + "\tReadLine: " + readLineNode.name + "\n");

        return str.toString();
    }

    @Override
    public String visitNullNode(NullNode nullNode){

        StringBuilder str = new StringBuilder();
        if(nullNode.operator.length() > 0){
            str.append(tabs.toString() + "Operator: " + nullNode.operator +  "\n");
        }
        str.append(tabs.toString() + "\tNull Value: " + nullNode.name + "\n");

        return str.toString();
    }





    public boolean matches(String var, String... s){

        for(int i = 0; i < s.length; i++){
            if(var.equals(s[i])){
                return true;
            }
        }

        return false;

    }


    private String stripWhiteSpace(String str){
        StringBuilder s = new StringBuilder(str);

        int i = 0;
        while( s.charAt(i) == '\t' || s.charAt(i) == ' '){
            i++;
        }
        return s.substring(i);

    }

}
