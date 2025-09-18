import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

class CircuitElement {
    ComponentType type;
    Rectangle2D shape;
    double gunny;
    Point2D start, end;
    Object nonInvertingInputNode;
    Object invertingInputNode;
    Object outputNode;

    public CircuitElement(ComponentType type, Point2D pos) {
        this.type = type;
        if(type == ComponentType.RESISTOR) {
            this.shape = new Rectangle2D.Double(pos.getX(), pos.getY(), 60, 20);
            this.gunny = 1000;
            this.start = pos;
            this.end = new Point2D.Double(pos.getX() + 60, pos.getY() + 10);
        } else if(type == ComponentType.INDUCTOR) {
            this.shape = new Rectangle2D.Double(pos.getX(), pos.getY(), 60, 20);
            this.gunny = 0.1;
            this.start = pos;
            this.end = new Point2D.Double(pos.getX() + 60, pos.getY() + 10);
        } else if(type == ComponentType.CAPACITOR) {
            this.shape = new Rectangle2D.Double(pos.getX(), pos.getY(), 60, 20);
            this.gunny = 0.0001;
            this.start = pos;
            this.end = new Point2D.Double(pos.getX() + 60, pos.getY() + 10);
        } else if(type == ComponentType.OP_AMP) {
            this.shape = new Rectangle2D.Double(pos.getX(), pos.getY(), 60, 40);
            this.nonInvertingInputNode = new Point2D.Double(pos.getX(), pos.getY() + 10);
            this.invertingInputNode = new Point2D.Double(pos.getX(), pos.getY() + 30);
            this.outputNode = new Point2D.Double(pos.getX() + 60, pos.getY() + 20);
            this.start = pos;
            this.end = new Point2D.Double(pos.getX() + 60, pos.getY() + 20);
        } else if(type == ComponentType.VOLTAGE_SOURCE) {
            this.shape = new Rectangle2D.Double(pos.getX(), pos.getY(), 40, 40);
            this.gunny = 12.0; // 기본 전압값 12V
            this.start = pos;
            this.end = new Point2D.Double(pos.getX() + 40, pos.getY() + 20);
        } else if(type == ComponentType.CURRENT_SOURCE) {
            this.shape = new Rectangle2D.Double(pos.getX(), pos.getY(), 40, 40);
            this.gunny = 1.0; // 기본 전류값 1A
            this.start = pos;
            this.end = new Point2D.Double(pos.getX() + 40, pos.getY() + 20);
        } else {
            this.start = pos;
            this.end = pos;
        }
    }

    public boolean isNearWire(Point2D p) {
        if(type != ComponentType.WIRE || start == null || end == null) return false;
        final double threshold = 8.0;
        double dist = ptSegDist(start, end, p);
        return dist <= threshold;
    }

    private double ptSegDist(Point2D a, Point2D b, Point2D p) {
        double px = b.getX() - a.getX();
        double py = b.getY() - a.getY();
        double temp = (px * px) + (py * py);
        if (temp < 1e-10) {
            double dx = p.getX() - a.getX();
            double dy = p.getY() - a.getY();
            return Math.sqrt(dx * dx + dy * dy);
        }
        double u = ((p.getX() - a.getX()) * px + (p.getY() - a.getY()) * py) / temp;
        u = Math.max(0, Math.min(1, u));
        double x = a.getX() + u * px;
        double y = a.getY() + u * py;
        double dx = x - p.getX();
        double dy = y - p.getY();
        return Math.sqrt(dx * dx + dy * dy);
    }
}


