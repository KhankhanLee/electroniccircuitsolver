import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

class CircuitEditor extends JPanel implements MouseListener, MouseMotionListener {
    private ArrayList<CircuitElement> elements = new ArrayList<>();
    private CircuitTool currentTool = CircuitTool.RESISTOR;
    private CircuitElement selectedElement;
    private CircuitElement tempWire;
    private boolean  deleteMode = false;
    private Map<Point2D, CircuitNode> nodes = new HashMap<>();

    public Map<Point2D, CircuitNode> getNodes() { return nodes; }

    public CircuitEditor() {
        setBackground(Color.WHITE);
        setPreferredSize(new Dimension(1000, 600));
        addMouseListener(this);
        addMouseMotionListener(this);
    }

    public void setTool(CircuitTool tool) { currentTool = tool; }

    public void deleteSelected() {
        if(selectedElement != null) {
            elements.remove(selectedElement);
            selectedElement = null;
            repaint();
        } else {
            deleteMode = true;
        }
    }

    public CircuitAnalysisResult analyzeCircuit(double voltage) {
        boolean hasInductor = elements.stream().anyMatch(e -> e.type == ComponentType.INDUCTOR);
        boolean hasCapacitor = elements.stream().anyMatch(e -> e.type == ComponentType.CAPACITOR);
        boolean hasOpAmp = elements.stream().anyMatch(e -> e.type == ComponentType.OP_AMP);

        double R = elements.stream().filter(e -> e.type == ComponentType.RESISTOR).mapToDouble(e -> e.gunny).sum();

        if(hasOpAmp) { analyzeOpAmps(); }

        if(hasInductor && !hasCapacitor) {
            double L = elements.stream().filter(e -> e.type == ComponentType.INDUCTOR).mapToDouble(e -> e.gunny).sum();
            double tau = (R > 0) ? L / R : 0.001;
            CircuitAnalysisResult result = new CircuitAnalysisResult(R, L, 0, tau, "RL");
            result.hasOpAmp = hasOpAmp;
            return result;
        } else if(hasCapacitor && !hasInductor) {
            double C = elements.stream().filter(e -> e.type == ComponentType.CAPACITOR).mapToDouble(e -> e.gunny).sum();
            double tau = R * C;
            CircuitAnalysisResult result = new CircuitAnalysisResult(R, 0, C, tau, "RC");
            result.hasOpAmp = hasOpAmp;
            return result;
        } else if (hasInductor && hasCapacitor) {
            double L = elements.stream().filter(e -> e.type == ComponentType.INDUCTOR).mapToDouble(e -> e.gunny).sum();
            double C = elements.stream().filter(e -> e.type == ComponentType.CAPACITOR).mapToDouble(e -> e.gunny).sum();
            double tau = (L > 0 && R > 0) ? 1 / (R / (2 * L)) : 0.001;
            CircuitAnalysisResult result = new CircuitAnalysisResult(R, L, C, tau, "RLC");
            result.hasOpAmp = hasOpAmp;
            return result;
        } else {
            CircuitAnalysisResult result = new CircuitAnalysisResult(R, 0, 0, 0, null);
            result.hasOpAmp = hasOpAmp;
            return result;
        }
    }

    private Map<Object, Double> nodeVoltages = new HashMap<>();
    private double getNodeVoltage(Object node) { return nodeVoltages.getOrDefault(node, 0.0); }
    private void setNodeVoltage(Object node, double voltage) { nodeVoltages.put(node, voltage); }

    private void analyzeOpAmps() {
        for(CircuitElement e : elements) {
            if(e.type == ComponentType.OP_AMP) { analyzeOpAmp(e); }
        }
    }
    private void analyzeOpAmp(CircuitElement opAmp) {
        double vPlus = getNodeVoltage(opAmp.nonInvertingInputNode);
        double vMinus = getNodeVoltage(opAmp.invertingInputNode);
        double gain = 1e5;
        double vOut = gain * (vPlus - vMinus);
        setNodeVoltage(opAmp.outputNode, vOut);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(new Color(220, 220, 220));
        for(int x = 0; x < getWidth(); x += 20) g2.drawLine(x, 0, x, getHeight());
        for(int y = 0; y < getHeight(); y += 20) g2.drawLine(0, y, getWidth(), y);

        for(CircuitElement e : elements) {
            g2.setColor(e == selectedElement ? Color.BLUE : Color.BLACK);
            if(e.type == ComponentType.RESISTOR) {
                drawResistor(g2, e.shape);
                g2.drawString(formatValue(e.gunny) + "Ω", (int)e.shape.getCenterX()-20, (int)e.shape.getY()-5);
            } else if(e.type == ComponentType.INDUCTOR) {
                drawInductor(g2, e.shape);
                g2.drawString(formatValue(e.gunny) + "H", (int)e.shape.getCenterX()-15, (int)e.shape.getY()-5);
            } else if(e.type == ComponentType.CAPACITOR) {
                drawCapacitor(g2, e.shape);
                g2.drawString(formatValue(e.gunny) + "F", (int)e.shape.getCenterX()-15, (int)e.shape.getY()-5);
            } else if(e.type == ComponentType.WIRE) {
                g2.draw(new Line2D.Double(e.start, e.end));
            } else if(e.type == ComponentType.OP_AMP) {
                drawOpAmp(g2, e.shape);
            }
        }

        if(tempWire != null && tempWire.start != null && tempWire.end != null) {
            g2.setColor(new Color(0, 0, 0, 100));
            g2.draw(new Line2D.Double(tempWire.start, tempWire.end));
        }
    }

    private void drawResistor(Graphics2D g2, Rectangle2D rect) {
        Path2D path = new Path2D.Double();
        path.moveTo(rect.getX(), rect.getCenterY());
        path.lineTo(rect.getX()+15, rect.getCenterY());
        path.lineTo(rect.getX()+20, rect.getY());
        path.lineTo(rect.getX()+25, rect.getMaxY());
        path.lineTo(rect.getX()+30, rect.getY());
        path.lineTo(rect.getX()+35, rect.getMaxY());
        path.lineTo(rect.getX()+40, rect.getY());
        path.lineTo(rect.getX()+45, rect.getCenterY());
        path.lineTo(rect.getMaxX(), rect.getCenterY());
        g2.draw(path);
    }

    private void drawInductor(Graphics2D g2, Rectangle2D rect) {
        int y = (int) rect.getCenterY();
        for(int i=0; i<5; i++) { g2.drawOval((int)rect.getX()+i*12, y-5, 10, 10); }
    }

    private void drawCapacitor(Graphics2D g2, Rectangle2D rect) {
        int y = (int) rect.getY();
        int x1 = (int)rect.getX()+20;
        int x2 = (int)rect.getX()+40;
        g2.drawLine(x1, y, x1, y+20);
        g2.drawLine(x2, y, x2, y+20);
        g2.drawLine((int)rect.getX(), y+10, x1, y+10);
        g2.drawLine(x2, y+10, (int)rect.getMaxX(), y+10);
    }

    private void drawOpAmp(Graphics2D g2, Rectangle2D rect) {
        Path2D path = new Path2D.Double();
        path.moveTo(rect.getX(), rect.getY());
        path.lineTo(rect.getMaxX(), rect.getCenterY());
        path.lineTo(rect.getX(), rect.getMaxY());
        path.closePath();
        g2.setColor(Color.BLACK);
        g2.draw(path);
        g2.drawString("OP Amp", (int)rect.getCenterX() - 20, (int)rect.getCenterY());
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        Point2D pos = snapToGrid(e.getPoint());
        if (deleteMode) {
            for (int i = elements.size() - 1; i >= 0; i--) {
                CircuitElement elem = elements.get(i);
                boolean hit = false;
                if(elem.shape != null && elem.shape.intersects(pos.getX() - 3, pos.getY() - 3, 6, 6)) { hit = true; }
                if(elem.type == ComponentType.WIRE && elem.isNearWire(e.getPoint())) { hit = true; }
                if(hit) {
                    elements.remove(i);
                    repaint();
                    deleteMode = false;
                    return;
                }
            }
            deleteMode = false;
        }

        if (e.getClickCount() == 2) {
            for(CircuitElement elem : elements) {
                if((elem.type == ComponentType.RESISTOR || elem.type == ComponentType.INDUCTOR || elem.type == ComponentType.CAPACITOR)
                    && elem.shape != null && elem.shape.intersects(pos.getX() - 3, pos.getY() - 3, 6, 6)) {
                    editGunny(elem);
                    return;
                }
            }
        } else {
            Point2D snapped = snapToGrid(e.getPoint());
            CircuitElement newElement = null;
            switch(currentTool) {
                case RESISTOR: newElement = new CircuitElement(ComponentType.RESISTOR, snapped); break;
                case INDUCTOR: newElement = new CircuitElement(ComponentType.INDUCTOR, snapped); break;
                case CAPACITOR: newElement = new CircuitElement(ComponentType.CAPACITOR, snapped); break;
                case OP_AMP:
                    newElement = new CircuitElement(ComponentType.OP_AMP, snapped);
                    newElement.shape = new Rectangle2D.Double(snapped.getX(), snapped.getY(), 60, 40);
                    break;
                default: break;
            }
            if (newElement != null) {
                elements.add(newElement);
                updateNodes(newElement);
                repaint();
            } else {
                JOptionPane.showMessageDialog(this, "회로 요소를 추가할 수 없습니다.");
            }
        }
    }

    private void editGunny(CircuitElement elem) {
        String msg = "값 입력:";
        if(elem.type == ComponentType.RESISTOR) msg = "저항값(Ω) 입력:";
        if(elem.type == ComponentType.INDUCTOR) msg = "인덕턴스(H) 입력:";
        if(elem.type == ComponentType.CAPACITOR) msg = "커패시턴스(F) 입력:";
        String input = JOptionPane.showInputDialog(this, msg, elem.gunny);
        try {
            double newValue = Double.parseDouble(input);
            if(newValue > 0) { elem.gunny = newValue; repaint(); }
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "올바른 숫자를 입력하세요!");
        }
    }

    @Override
    public void mousePressed(MouseEvent e) {
        Point2D pos = snapToGrid(e.getPoint());
        if(currentTool == CircuitTool.WIRE) {
            tempWire = new CircuitElement(ComponentType.WIRE, pos);
            tempWire.start = pos;
        } else {
            selectedElement = null;
            for(CircuitElement elem : elements) {
                if(elem.shape != null && elem.shape.contains(pos)) { selectedElement = elem; break; }
            }
        }
        repaint();
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        if(currentTool == CircuitTool.WIRE && tempWire != null) {
            tempWire.end = snapToGrid(e.getPoint());
            repaint();
        } else if(selectedElement != null) {
            Point2D newPos = snapToGrid(e.getPoint());
            if(selectedElement.shape != null)
                selectedElement.shape.setRect(newPos.getX(), newPos.getY(), selectedElement.shape.getWidth(), selectedElement.shape.getHeight());
            repaint();
        }
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        if(currentTool == CircuitTool.WIRE && tempWire != null) {
            tempWire.end = snapToGrid(e.getPoint());
            elements.add(tempWire);
            updateNodes(tempWire);
            tempWire = null;
            repaint();
        }
    }

    private void updateNodes(CircuitElement element) {
        addToNode(element.start, element);
        addToNode(element.end, element);
    }
    private void addToNode(Point2D pos, CircuitElement elem) {
        CircuitNode node = nodes.computeIfAbsent(pos, k -> {
            CircuitNode newNode = new CircuitNode();
            newNode.position = pos;
            return newNode;
        });
        node.connectedElements.add(elem);
    }

    private Point2D snapToGrid(Point2D p) {
        int x = ((int)p.getX() / 20) * 20;
        int y = ((int)p.getY() / 20) * 20;
        return new Point2D.Double(x, y);
    }

    private String formatValue(double value) {
        if (value >= 1000) { return String.format("%.0f", value); }
        else if (value >= 1) { return String.format("%.2f", value); }
        else if (value >= 0.01) { return String.format("%.4f", value); }
        else if (value >= 0.0001) { return String.format("%.6f", value); }
        else {
            if (value >= 1e-6) { return String.format("%.2fμ", value * 1e6); }
            else if (value >= 1e-9) { return String.format("%.2fn", value * 1e9); }
            else if (value >= 1e-12) { return String.format("%.2fp", value * 1e12); }
            else { return String.format("%.2e", value); }
        }
    }

    @Override public void mouseMoved(MouseEvent e) {}
    @Override public void mouseEntered(MouseEvent e) {}
    @Override public void mouseExited(MouseEvent e) {}
}


