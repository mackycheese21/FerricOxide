package mackycheese21.ferricoxide.ast.expr;

import mackycheese21.ferricoxide.ast.visitor.ExpressionVisitor;

public class AccessVar extends Expression {

    public final String name;

    public AccessVar(String name) {
        super(false);
        this.name = name;
    }

    @Override
    public Expression makeLValue() {
        return new RefAccessVar(name);
    }

    @Override
    public <T> T visit(ExpressionVisitor<T> visitor) {
        return visitor.visitAccessVar(this);
    }
}
