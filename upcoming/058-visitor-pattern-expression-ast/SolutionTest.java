import java.util.Map;
import java.util.Random;
import java.util.Set;

public class SolutionTest {
    private static int passed = 0;

    interface TestBody {
        void run() throws Exception;
    }

    static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    static void expectThrows(Class<? extends Throwable> type, Runnable r) {
        try {
            r.run();
        } catch (Throwable t) {
            if (type.isInstance(t)) return;
            throw new AssertionError("expected " + type.getSimpleName() + " but got " + t, t);
        }
        throw new AssertionError("expected " + type.getSimpleName() + " to be thrown");
    }

    static void run(String name, TestBody body) {
        try {
            body.run();
        } catch (Throwable t) {
            throw new AssertionError(name + " failed: " + t.getMessage(), t);
        }
        passed++;
    }

    static Expr x = new Var("x"), y = new Var("y");

    static Expr n(double v) {
        return new Num(v);
    }

    static double eval(Expr e, double xv) {
        return e.accept(new Evaluator(Map.of("x", xv, "y", 2.0)));
    }

    static String print(Expr e) {
        return e.accept(new Printer());
    }

    static Expr randomExpr(Random rnd, int depth) {
        if (depth == 0 || rnd.nextInt(4) == 0) {
            return switch (rnd.nextInt(3)) {
                case 0 -> n(rnd.nextInt(7) - 3);
                case 1 -> x;
                default -> y;
            };
        }
        return switch (rnd.nextInt(4)) {
            case 0 -> new Add(randomExpr(rnd, depth - 1), randomExpr(rnd, depth - 1));
            case 1 -> new Mul(randomExpr(rnd, depth - 1), randomExpr(rnd, depth - 1));
            case 2 -> new Neg(randomExpr(rnd, depth - 1));
            default -> new Pow(randomExpr(rnd, depth - 1), rnd.nextInt(4));
        };
    }

    public static void main(String[] args) {
        // 3x^2 + 2x - 5
        Expr poly = new Add(new Add(new Mul(n(3), new Pow(x, 2)), new Mul(n(2), x)), new Neg(n(5)));

        run("evaluate", () -> {
            check(eval(poly, 2) == 11, "3*4 + 4 - 5");
            check(eval(new Mul(x, y), 3) == 6, "x*y");
            expectThrows(IllegalArgumentException.class, () -> new Var("z").accept(new Evaluator(Map.of())));
        });

        run("printMinimalParens", () -> {
            check(print(poly).equals("3 * x^2 + 2 * x + -5"), print(poly));
            check(print(new Mul(new Add(x, n(1)), y)).equals("(x + 1) * y"), "needs parens");
            check(print(new Add(x, new Mul(n(2), y))).equals("x + 2 * y"), "no parens");
            check(print(new Pow(new Add(x, n(1)), 3)).equals("(x + 1)^3"), "pow base");
            check(print(new Neg(new Add(x, y))).equals("-(x + y)"), "neg of sum");
            check(print(n(2.5)).equals("2.5"), "decimal");
        });

        run("derivativeOfPolynomial", () -> {
            Expr d = poly.accept(new Derivative("x")).accept(new Simplifier());
            for (double v : new double[] {-2, 0, 1.5, 10}) check(eval(d, v) == 6 * v + 2, "6x + 2 at " + v);
        });

        run("derivativeTreatsOtherVarsAsConstants", () -> {
            Expr e = new Mul(x, y); // d/dx = y
            Expr d = e.accept(new Derivative("x")).accept(new Simplifier());
            check(d.equals(y), print(d));
            check(n(7).accept(new Derivative("x")).equals(n(0)), "constant");
        });

        run("simplifierIdentities", () -> {
            Simplifier s = new Simplifier();
            check(new Add(x, n(0)).accept(s).equals(x), "x+0");
            check(new Mul(n(1), x).accept(s).equals(x), "1*x");
            check(new Mul(x, n(0)).accept(s).equals(n(0)), "x*0");
            check(new Neg(new Neg(x)).accept(s).equals(x), "--x");
            check(new Add(n(2), new Mul(n(3), n(4))).accept(s).equals(n(14)), "fold");
            check(new Pow(x, 0).accept(s).equals(n(1)) && new Pow(x, 1).accept(s).equals(x), "pow 0/1");
        });

        run("variableCollector", () -> {
            check(poly.accept(new VariableCollector()).equals(Set.of("x")), "poly vars");
            check(new Add(y, new Mul(x, new Var("a"))).accept(new VariableCollector()).toString().equals("[a, x, y]"),
                    "sorted");
            check(n(1).accept(new VariableCollector()).isEmpty(), "none");
        });

        run("invalidPow", () -> {
            expectThrows(IllegalArgumentException.class, () -> new Pow(x, -1));
        });

        run("randomizedSimplifyPreservesValue", () -> {
            Random rnd = new Random(11);
            for (int t = 0; t < 500; t++) {
                Expr e = randomExpr(rnd, 5);
                Expr s = e.accept(new Simplifier());
                double xv = rnd.nextDouble() * 4 - 2;
                double a = eval(e, xv), b = eval(s, xv);
                check(Math.abs(a - b) <= 1e-9 * Math.max(1, Math.abs(a)), print(e) + " vs " + print(s));
            }
        });

        run("randomizedDerivativeMatchesFiniteDifference", () -> {
            Random rnd = new Random(12);
            for (int t = 0; t < 500; t++) {
                Expr e = randomExpr(rnd, 4);
                Expr d = e.accept(new Derivative("x"));
                double xv = rnd.nextDouble() * 2 - 1, h = 1e-5;
                double numeric = (eval(e, xv + h) - eval(e, xv - h)) / (2 * h);
                double symbolic = eval(d, xv);
                check(Math.abs(numeric - symbolic) <= 1e-4 * Math.max(1, Math.abs(symbolic)),
                        print(e) + ": " + numeric + " vs " + symbolic);
            }
        });

        System.out.println("All " + passed + " tests passed");
    }
}
