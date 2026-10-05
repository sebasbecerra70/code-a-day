import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/** Expression AST. Each node's accept() double-dispatches to the matching visit method. */
sealed interface Expr permits Num, Var, Add, Mul, Neg, Pow {
    <R> R accept(ExprVisitor<R> v);
}

record Num(double value) implements Expr {
    public <R> R accept(ExprVisitor<R> v) { return v.visitNum(this); }
}

record Var(String name) implements Expr {
    public <R> R accept(ExprVisitor<R> v) { return v.visitVar(this); }
}

record Add(Expr left, Expr right) implements Expr {
    public <R> R accept(ExprVisitor<R> v) { return v.visitAdd(this); }
}

record Mul(Expr left, Expr right) implements Expr {
    public <R> R accept(ExprVisitor<R> v) { return v.visitMul(this); }
}

record Neg(Expr operand) implements Expr {
    public <R> R accept(ExprVisitor<R> v) { return v.visitNeg(this); }
}

/** base ^ n for a constant integer exponent n >= 0. */
record Pow(Expr base, int exponent) implements Expr {
    Pow {
        if (exponent < 0) throw new IllegalArgumentException("exponent must be >= 0");
    }

    public <R> R accept(ExprVisitor<R> v) { return v.visitPow(this); }
}

/** One method per node type. Adding an operation means adding a visitor class, with no changes to the nodes. */
interface ExprVisitor<R> {
    R visitNum(Num n);
    R visitVar(Var v);
    R visitAdd(Add a);
    R visitMul(Mul m);
    R visitNeg(Neg n);
    R visitPow(Pow p);
}

/** Evaluates with variable bindings. */
class Evaluator implements ExprVisitor<Double> {
    private final Map<String, Double> env;

    Evaluator(Map<String, Double> env) {
        this.env = env;
    }

    public Double visitNum(Num n) { return n.value(); }

    public Double visitVar(Var v) {
        Double x = env.get(v.name());
        if (x == null) throw new IllegalArgumentException("unbound variable: " + v.name());
        return x;
    }

    public Double visitAdd(Add a) { return a.left().accept(this) + a.right().accept(this); }
    public Double visitMul(Mul m) { return m.left().accept(this) * m.right().accept(this); }
    public Double visitNeg(Neg n) { return -n.operand().accept(this); }
    public Double visitPow(Pow p) { return Math.pow(p.base().accept(this), p.exponent()); }
}

/** Pretty-prints with the minimum parentheses, based on operator precedence. */
class Printer implements ExprVisitor<String> {
    // Higher binds tighter.
    private static int prec(Expr e) {
        return switch (e) {
            case Add a -> 1;
            case Mul m -> 2;
            case Neg n -> 3;
            case Pow p -> 4;
            case Num n -> n.value() < 0 ? 3 : 5; // a negative literal behaves like a unary minus
            case Var v -> 5;
        };
    }

    private String wrap(Expr child, int minPrec) {
        String s = child.accept(this);
        return prec(child) < minPrec ? "(" + s + ")" : s;
    }

    public String visitNum(Num n) {
        double v = n.value();
        return v == Math.rint(v) && Math.abs(v) < 1e15 ? Long.toString((long) v) : Double.toString(v);
    }

    public String visitVar(Var v) { return v.name(); }
    // Add and Mul are associative, so the right side needs parentheses only if it binds strictly looser.
    public String visitAdd(Add a) { return wrap(a.left(), 1) + " + " + wrap(a.right(), 1); }
    public String visitMul(Mul m) { return wrap(m.left(), 2) + " * " + wrap(m.right(), 2); }
    public String visitNeg(Neg n) { return "-" + wrap(n.operand(), 4); }
    public String visitPow(Pow p) { return wrap(p.base(), 5) + "^" + p.exponent(); }
}

/** Symbolic derivative with respect to one variable. The result is unsimplified. */
class Derivative implements ExprVisitor<Expr> {
    private final String x;

    Derivative(String x) {
        this.x = x;
    }

    public Expr visitNum(Num n) { return new Num(0); }
    public Expr visitVar(Var v) { return new Num(v.name().equals(x) ? 1 : 0); }
    public Expr visitAdd(Add a) { return new Add(a.left().accept(this), a.right().accept(this)); }

    // Product rule: (fg)' = f'g + fg'
    public Expr visitMul(Mul m) {
        return new Add(new Mul(m.left().accept(this), m.right()), new Mul(m.left(), m.right().accept(this)));
    }

    public Expr visitNeg(Neg n) { return new Neg(n.operand().accept(this)); }

    // Power rule plus chain rule: (f^n)' = n * f^(n-1) * f'
    public Expr visitPow(Pow p) {
        if (p.exponent() == 0) return new Num(0);
        return new Mul(new Mul(new Num(p.exponent()), new Pow(p.base(), p.exponent() - 1)), p.base().accept(this));
    }
}

/** Bottom-up algebraic simplification: constant folding and identities such as x+0, x*1, x*0, --x. */
class Simplifier implements ExprVisitor<Expr> {
    public Expr visitNum(Num n) { return n; }
    public Expr visitVar(Var v) { return v; }

    public Expr visitAdd(Add a) {
        Expr l = a.left().accept(this), r = a.right().accept(this);
        if (l instanceof Num ln && r instanceof Num rn) return new Num(ln.value() + rn.value());
        if (isConst(l, 0)) return r;
        if (isConst(r, 0)) return l;
        return new Add(l, r);
    }

    public Expr visitMul(Mul m) {
        Expr l = m.left().accept(this), r = m.right().accept(this);
        if (l instanceof Num ln && r instanceof Num rn) return new Num(ln.value() * rn.value());
        if (isConst(l, 0) || isConst(r, 0)) return new Num(0);
        if (isConst(l, 1)) return r;
        if (isConst(r, 1)) return l;
        return new Mul(l, r);
    }

    public Expr visitNeg(Neg n) {
        Expr o = n.operand().accept(this);
        if (o instanceof Num on) return new Num(-on.value());
        if (o instanceof Neg inner) return inner.operand();
        return new Neg(o);
    }

    public Expr visitPow(Pow p) {
        Expr b = p.base().accept(this);
        if (p.exponent() == 0) return new Num(1);
        if (p.exponent() == 1) return b;
        if (b instanceof Num bn) return new Num(Math.pow(bn.value(), p.exponent()));
        return new Pow(b, p.exponent());
    }

    private static boolean isConst(Expr e, double v) {
        return e instanceof Num n && n.value() == v;
    }
}

/** Collects variable names in sorted order. */
class VariableCollector implements ExprVisitor<Set<String>> {
    public Set<String> visitNum(Num n) { return new TreeSet<>(); }
    public Set<String> visitVar(Var v) { return new TreeSet<>(Set.of(v.name())); }
    public Set<String> visitAdd(Add a) { return union(a.left(), a.right()); }
    public Set<String> visitMul(Mul m) { return union(m.left(), m.right()); }
    public Set<String> visitNeg(Neg n) { return n.operand().accept(this); }
    public Set<String> visitPow(Pow p) { return p.base().accept(this); }

    private Set<String> union(Expr a, Expr b) {
        Set<String> s = a.accept(this);
        s.addAll(b.accept(this));
        return s;
    }
}
