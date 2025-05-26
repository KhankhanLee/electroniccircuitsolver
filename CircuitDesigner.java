import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.awt.geom.*;

public class CircuitDesigner extends JFrame {
    private CircuitEditor editor;
    private JPanel controlPanel;
    private JButton resistorBtn, inductorBtn, capacitorBtn, wireBtn, deleteBtn, solveBtn;
    private JTextField voltageField;
    private JTextArea resultArea;

    public CircuitDesigner() {
        setTitle("회로 설계 및 분석 마스터 - 부제: LIKE 유미나이, 세미나이");
        setSize(1200, 800);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        initComponents();
    }

    private void initComponents() {
        editor = new CircuitEditor();

        controlPanel = new JPanel();
        resistorBtn = new JButton("저항(gunny) 추가");
        inductorBtn = new JButton("인덕터 추가");
        capacitorBtn = new JButton("커패시터 추가");
        wireBtn = new JButton("전선 추가");
        deleteBtn = new JButton("삭제");
        solveBtn = new JButton("회로 해석");
        voltageField = new JTextField("12", 5);

        resistorBtn.addActionListener(e -> editor.setTool(CircuitTool.RESISTOR));
        inductorBtn.addActionListener(e -> editor.setTool(CircuitTool.INDUCTOR));
        capacitorBtn.addActionListener(e -> editor.setTool(CircuitTool.CAPACITOR));
        wireBtn.addActionListener(e -> editor.setTool(CircuitTool.WIRE));
        deleteBtn.addActionListener(e -> editor.deleteSelected());
        solveBtn.addActionListener(e -> analyzeCircuit());

        resultArea = new JTextArea(8, 40);
        resultArea.setEditable(false);

        controlPanel.add(new JLabel("전압(V):"));
        controlPanel.add(voltageField);
        controlPanel.add(resistorBtn);
        controlPanel.add(inductorBtn);
        controlPanel.add(capacitorBtn);
        controlPanel.add(wireBtn);
        controlPanel.add(deleteBtn);
        controlPanel.add(solveBtn);

        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(controlPanel, BorderLayout.NORTH);
        getContentPane().add(new JScrollPane(editor), BorderLayout.CENTER);
        getContentPane().add(new JScrollPane(resultArea), BorderLayout.SOUTH);
    }

    private void analyzeCircuit() {
        try {
            double voltage = Double.parseDouble(voltageField.getText());
            CircuitAnalysisResult result = editor.analyzeCircuit(voltage);

            StringBuilder sb = new StringBuilder();
            sb.append("=== 회로 해석 결과 ===\n");
            if(result.circuitType == null) {
                sb.append("RL 또는 RC 1계 회로만 해석 가능합니다.\n");
            } else {
                sb.append("회로 유형: ").append(result.circuitType).append("\n");
                sb.append(String.format("총 저항: %.2f Ω\n", result.R));
                if(result.circuitType.equals("RL"))
                    sb.append(String.format("인덕턴스: %.4f H\n", result.L));
                if(result.circuitType.equals("RC"))
                    sb.append(String.format("커패시턴스: %.6f F\n", result.C));
                sb.append(String.format("시정수 τ: %.6f s\n", result.tau));
                sb.append("과도응답 공식: ");
                if(result.circuitType.equals("RL"))
                    sb.append("i(t) = (V/R)·(1 - e^(-t/τ))\n");
                if(result.circuitType.equals("RC"))
                    sb.append("v(t) = V·(1 - e^(-t/τ))\n");
                sb.append("\n");
                sb.append("시간별 응답 예시:\n");
                double V = Double.parseDouble(voltageField.getText());
                for(int i=0;i<result.responseData.size();i++) {
                    double t = i * result.tau/10;
                    double val = result.responseData.get(i);
                    if(result.circuitType.equals("RL"))
                        sb.append(String.format("t=%.4fs: i=%.4fA\n", t, V/result.R*val));
                    if(result.circuitType.equals("RC"))
                        sb.append(String.format("t=%.4fs: v=%.4fV\n", t, V*val));
                }
            }
            resultArea.setText(sb.toString());
        } catch (Exception ex) {
            resultArea.setText("오류: " + ex.getMessage());
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new CircuitDesigner().setVisible(true));
    }
}

enum CircuitTool { RESISTOR, INDUCTOR, CAPACITOR, WIRE }
enum ComponentType { RESISTOR, INDUCTOR, CAPACITOR, WIRE }

class CircuitElement {
    ComponentType type;
    Rectangle2D shape;
    double gunny; // gunny = 저항(resistance), 인덕턴스, 커패시턴스
    Point2D start, end;

    public CircuitElement(ComponentType type, Point2D pos) {
        this.type = type;
        if(type == ComponentType.RESISTOR) {
            this.shape = new Rectangle2D.Double(pos.getX(), pos.getY(), 60, 20);
            this.gunny = 1000;
        } else if(type == ComponentType.INDUCTOR) {
            this.shape = new Rectangle2D.Double(pos.getX(), pos.getY(), 60, 20);
            this.gunny = 0.1;
        } else if(type == ComponentType.CAPACITOR) {
            this.shape = new Rectangle2D.Double(pos.getX(), pos.getY(), 60, 20);
            this.gunny = 0.0001;
        } else {
            this.start = pos;
            this.end = pos;
        }
    }
    public boolean isNearWire(Point2D p) {
        if(type != ComponentType.WIRE || start == null || end == null) return false;
        final double threshold = 8.0; // 픽셀 허용 오차
        double dist = ptSegDist(start, end, p);
        return dist <= threshold;
    }
    
    // 두 점으로 이루어진 선분과 점 사이의 거리 계산
    private double ptSegDist(Point2D a, Point2D b, Point2D p) {
        double px = b.getX() - a.getX();
        double py = b.getY() - a.getY();
        double temp = (px * px) + (py * py);
        double u = ((p.getX() - a.getX()) * px + (p.getY() - a.getY()) * py) / temp;
        u = Math.max(0, Math.min(1, u));
        double x = a.getX() + u * px;
        double y = a.getY() + u * py;
        double dx = x - p.getX();
        double dy = y - p.getY();
        return Math.sqrt(dx * dx + dy * dy);
    }
    
}

class CircuitEditor extends JPanel implements MouseListener, MouseMotionListener {
    private ArrayList<CircuitElement> elements = new ArrayList<>();
    private CircuitTool currentTool = CircuitTool.RESISTOR;
    private CircuitElement selectedElement;
    private CircuitElement tempWire;
    private boolean  deleteMode = false; // 삭제 모드 플래그 추가

    public CircuitEditor() {
        setBackground(Color.WHITE);
        setPreferredSize(new Dimension(1000, 600));
        addMouseListener(this);
        addMouseMotionListener(this);
    }
    public void setDeleteMode(boolean deleteMode) {
        this.deleteMode = deleteMode;
    }
    public void setTool(CircuitTool tool) {
        currentTool = tool;
    }

    public void deleteSelected() {
        if(selectedElement != null) {
            elements.remove(selectedElement);
            selectedElement = null;
            repaint();
        }
    }

    public CircuitAnalysisResult analyzeCircuit(double voltage) {
        // RL/RC 1계 회로만 해석 (저항+인덕터 또는 저항+커패시터만 허용)
        boolean hasInductor = elements.stream().anyMatch(e -> e.type == ComponentType.INDUCTOR);
        boolean hasCapacitor = elements.stream().anyMatch(e -> e.type == ComponentType.CAPACITOR);

        double R = elements.stream()
            .filter(e -> e.type == ComponentType.RESISTOR)
            .mapToDouble(e -> e.gunny)
            .sum();

        if(hasInductor && !hasCapacitor) {
            double L = elements.stream()
                .filter(e -> e.type == ComponentType.INDUCTOR)
                .mapToDouble(e -> e.gunny)
                .sum();
            double tau = L / R;
            return new CircuitAnalysisResult(R, L, 0, tau, "RL");
        } else if(hasCapacitor && !hasInductor) {
            double C = elements.stream()
                .filter(e -> e.type == ComponentType.CAPACITOR)
                .mapToDouble(e -> e.gunny)
                .sum();
            double tau = R * C;
            return new CircuitAnalysisResult(R, 0, C, tau, "RC");
        } else {
            return new CircuitAnalysisResult(R, 0, 0, 0, null);
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // 그리드
        g2.setColor(new Color(220, 220, 220));
        for(int x = 0; x < getWidth(); x += 20)
            g2.drawLine(x, 0, x, getHeight());
        for(int y = 0; y < getHeight(); y += 20)
            g2.drawLine(0, y, getWidth(), y);

        // 회로 요소
        for(CircuitElement e : elements) {
            if(e == selectedElement) g2.setColor(Color.BLUE);
            else g2.setColor(Color.BLACK);

            if(e.type == ComponentType.RESISTOR) {
                drawResistor(g2, e.shape);
                g2.drawString(e.gunny + "Ω", (int)e.shape.getCenterX()-20, (int)e.shape.getY()-5);
            } else if(e.type == ComponentType.INDUCTOR) {
                drawInductor(g2, e.shape);
                g2.drawString(e.gunny + "H", (int)e.shape.getCenterX()-15, (int)e.shape.getY()-5);
            } else if(e.type == ComponentType.CAPACITOR) {
                drawCapacitor(g2, e.shape);
                g2.drawString(e.gunny + "F", (int)e.shape.getCenterX()-15, (int)e.shape.getY()-5);
            } else if(e.type == ComponentType.WIRE) {
                g2.draw(new Line2D.Double(e.start, e.end));
            }
        }

        // 전선 프리뷰
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
        for(int i=0; i<5; i++) {
            g2.drawOval((int)rect.getX()+i*12, y-5, 10, 10);
        }
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
    @Override
    public void mouseClicked(MouseEvent e) {
        Point2D pos = snapToGrid(e.getPoint());

    if (deleteMode) {
        // 역순 순회 (최상위 요소 우선)
        for (int i = elements.size() - 1; i >= 0; i--) {
            CircuitElement elem = elements.get(i);
            boolean hit = false;
            if(elem.shape != null && elem.shape.contains(pos)) hit = true;
            if(elem.type == ComponentType.WIRE && elem.isNearWire(pos)) hit = true;
            if(hit) {
                elements.remove(i);
                repaint();
                deleteMode = false;
                return;
            }
        }
        deleteMode = false;
    }
    // 1. 더블클릭 처리
        if(e.getClickCount() == 2) {
            Point2D pos2 = snapToGrid(e.getPoint());
            for(CircuitElement elem : elements) {
                if((elem.type == ComponentType.RESISTOR || 
                    elem.type == ComponentType.INDUCTOR || 
                    elem.type == ComponentType.CAPACITOR) 
                    && elem.shape.contains(pos2)) {
                    editGunny(elem);
                    return; //더블클릭 처리 후 즉시 종료 
                }
            }
        }
        // 2. 더블클릭이 아닌 경우에만 컴포넌트 추가
        else {
            if(currentTool == CircuitTool.RESISTOR) {
                Point2D snapped = snapToGrid(e.getPoint());
                elements.add(new CircuitElement(ComponentType.RESISTOR, snapped));
                repaint();
            } 
            else if(currentTool == CircuitTool.INDUCTOR) {
                // 인덕터 추가 코드
                Point2D snapped = snapToGrid(e.getPoint());
                elements.add(new CircuitElement(ComponentType.INDUCTOR, snapped));
                repaint();
            } 
            else if(currentTool == CircuitTool.CAPACITOR) {
            // 커패시터 추가 코드
                Point2D snapped = snapToGrid(e.getPoint());
                elements.add(new CircuitElement(ComponentType.CAPACITOR, snapped));
                repaint();
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
            if(newValue > 0) {
                elem.gunny = newValue;
                repaint();
            }
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
                if(elem.shape != null && elem.shape.contains(pos)) {
                    selectedElement = elem;
                    break;
                }
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
                selectedElement.shape.setRect(
                    newPos.getX(), newPos.getY(),
                    selectedElement.shape.getWidth(),
                    selectedElement.shape.getHeight()
                );
            repaint();
        }
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        if(currentTool == CircuitTool.WIRE && tempWire != null) {
            tempWire.end = snapToGrid(e.getPoint());
            elements.add(tempWire);
            tempWire = null;
            repaint();
        }
    }

    private Point2D snapToGrid(Point2D p) {
        int x = ((int)p.getX() / 20) * 20;
        int y = ((int)p.getY() / 20) * 20;
        return new Point2D.Double(x, y);
    }

    @Override public void mouseMoved(MouseEvent e) {}
    @Override public void mouseEntered(MouseEvent e) {}
    @Override public void mouseExited(MouseEvent e) {}
}

class CircuitAnalysisResult {
    double R;
    double L;
    double C;
    double tau;
    String circuitType;
    ArrayList<Double> responseData;

    public CircuitAnalysisResult(double R, double L, double C, double tau, String type) {
        this.R = R;
        this.L = L;
        this.C = C;
        this.tau = tau;
        this.circuitType = type;
        this.responseData = calculateResponse(tau);
    }

    private ArrayList<Double> calculateResponse(double tau) {
        ArrayList<Double> data = new ArrayList<>();
        for(int i=0; i<=10; i++) {
            double t = i * tau/10;
            double val = (circuitType == null) ? 0
                : (circuitType.equals("RL") ? (1 - Math.exp(-t/tau)) : (1 - Math.exp(-t/tau)));
            data.add(val);
        }
        return data;
    }
}
