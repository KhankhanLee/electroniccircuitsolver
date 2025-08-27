import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.awt.geom.*;
import java.util.Map;
import java.util.HashMap;
import java.util.List;

public class CircuitDesigner extends JFrame {
    private CircuitEditor editor;
    private JPanel controlPanel;
    private JButton resistorBtn, inductorBtn, capacitorBtn, wireBtn,OP_AMPBtn, deleteBtn, solveBtn;
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
        OP_AMPBtn = new JButton("OPAMP 추가");
        deleteBtn = new JButton("삭제");
        solveBtn = new JButton("회로 해석");
        voltageField = new JTextField("12", 5);

        resistorBtn.addActionListener(e -> editor.setTool(CircuitTool.RESISTOR));
        inductorBtn.addActionListener(e -> editor.setTool(CircuitTool.INDUCTOR));
        capacitorBtn.addActionListener(e -> editor.setTool(CircuitTool.CAPACITOR));
        wireBtn.addActionListener(e -> editor.setTool(CircuitTool.WIRE));
        OP_AMPBtn.addActionListener(e -> editor.setTool(CircuitTool.OP_AMP));
        deleteBtn.addActionListener(e -> editor.deleteSelected());
        solveBtn.addActionListener(e -> analyzeCircuit());

        resultArea = new JTextArea(8, 40);
        resultArea.setEditable(false);

        controlPanel.add(new JLabel("전압(V):"));
        controlPanel.add(voltageField);
        controlPanel.add(resistorBtn);
        controlPanel.add(inductorBtn);
        controlPanel.add(capacitorBtn);
        controlPanel.add(OP_AMPBtn);
        controlPanel.add(wireBtn);
        controlPanel.add(deleteBtn);
        controlPanel.add(solveBtn);

        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(controlPanel, BorderLayout.NORTH);
        getContentPane().add(new JScrollPane(editor), BorderLayout.CENTER);
        getContentPane().add(new JScrollPane(resultArea), BorderLayout.SOUTH);
    }
    
    // 값 포맷팅 메서드 - 과학적 표기법을 일반 표기법으로 변환
    private String formatValue(double value) {
        if (value >= 1000) {
            return String.format("%.0f", value);
        } else if (value >= 1) {
            return String.format("%.2f", value);
        } else if (value >= 0.01) {
            return String.format("%.4f", value);
        } else if (value >= 0.0001) {
            return String.format("%.6f", value);
        } else {
            // 매우 작은 값의 경우 μ, n, p 단위 사용
            if (value >= 1e-6) {
                return String.format("%.2fμ", value * 1e6);
            } else if (value >= 1e-9) {
                return String.format("%.2fn", value * 1e9);
            } else if (value >= 1e-12) {
                return String.format("%.2fp", value * 1e12);
            } else {
                return String.format("%.2e", value);
            }
        }
    }
    private void analyzeCircuit() {
        try {
            double voltage = Double.parseDouble(voltageField.getText());
            CircuitAnalysisResult result = editor.analyzeCircuit(voltage);
            StringBuilder sb = new StringBuilder();
            sb.append("=== 회로 해석 결과 ===\n");
            if (result.circuitType == null && !result.hasOpAmp) {
            sb.append("해석 가능한 회로 유형이 아닙니다.\n(RL, RC, RLC, OP-AMP 회로 해석 가능)\n");
        } else {
            sb.append("회로 유형: ").append(result.circuitType).append("\n");
            sb.append(String.format("총 등가 저항: %.2f Ω\n", result.R));
            if (result.circuitType.contains("RL"))
                sb.append(String.format("총 등가 인덕턴스: %s H\n", formatValue(result.L)));
            if (result.circuitType.contains("RC"))
                sb.append(String.format("총 등가 커패시턴스: %s F\n", formatValue(result.C)));

            if ("RLC".equals(result.circuitType)) {
                 sb.append(String.format("감쇠 계수 α: %.4f\n", result.alpha));
                 sb.append(String.format("공진 주파수 ω₀: %.4f rad/s\n", result.omega0));
                 sb.append("응답 유형: ").append(result.dampingType).append("\n");

            } else if ("RL".equals(result.circuitType) || "RC".equals(result.circuitType)) {
                sb.append(String.format("시정수 τ: %.6f s\n", result.tau));
                sb.append("과도응답 공식:\n");
                if ("RL".equals(result.circuitType))
                    sb.append("  i(t) = (V/R) * (1 - e^(-t/τ))\n");
                if ("RC".equals(result.circuitType))
                    sb.append("  v_C(t) = V * (1 - e^(-t/τ))\n");
            }
             if(result.hasOpAmp) {
                sb.append("\n[OP-AMP 발견]\n");
                sb.append("OP-AMP 해석은 현재 지원되지 않으나, 회로 내에 존재합니다.\n");
            }
        }
        // 병렬 탐지 결과 표시
        Map<Point2D, ParallelGroup> parallelGroups = result.detectParallels(editor.getNodes());
        if (!parallelGroups.isEmpty()) {
            sb.append("\n[병렬 연결 감지 결과]\n");
            for (Map.Entry<Point2D, ParallelGroup> entry : parallelGroups.entrySet()) {
                Point2D p = entry.getKey();
                ParallelGroup group = entry.getValue();
                sb.append(String.format("● 노드 (%.0f, %.0f): ", p.getX(), p.getY()));
                List<String> parts = new ArrayList<>();
                if (!group.resistors.isEmpty()) parts.add("저항 " + group.resistors.size() + "개");
                if (!group.capacitors.isEmpty()) parts.add("커패시터 " + group.capacitors.size() + "개");
                if (!group.inductors.isEmpty()) parts.add("인덕터 " + group.inductors.size() + "개");
                sb.append(String.join(", ", parts)).append("\n");
            }
        }
        resultArea.setText(sb.toString());
        } catch (Exception ex) {
            resultArea.setText("오류: " + ex.getMessage());
            ex.printStackTrace();
        }
    }
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new CircuitDesigner().setVisible(true));
    }
}

enum CircuitTool { RESISTOR, INDUCTOR, CAPACITOR, WIRE, OP_AMP }
enum ComponentType { RESISTOR, INDUCTOR, CAPACITOR, WIRE, OP_AMP }

class CircuitElement {
    ComponentType type;
    Rectangle2D shape;
    double gunny; // gunny = 저항(resistance), 인덕턴스, 커패시턴스
    Point2D start, end;
    // OP Amp 전용 노드
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
        
        // 0으로 나누기 방지
        if (temp < 1e-10) {
            // 두 점이 거의 같은 위치에 있는 경우
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

class CircuitEditor extends JPanel implements MouseListener, MouseMotionListener {
    private ArrayList<CircuitElement> elements = new ArrayList<>();
    private CircuitTool currentTool = CircuitTool.RESISTOR;
    private CircuitElement selectedElement;
    private CircuitElement tempWire;
    private boolean  deleteMode = false; // 삭제 모드 플래그 추가
    private Map<Point2D, CircuitNode> nodes = new HashMap<>();
    private void updateNodes(CircuitElement element) {
        // 요소의 시작점과 끝점을 노드로 등록
        addToNode(element.start, element);
        addToNode(element.end, element);
    }
    private void addToNode(Point2D pos, CircuitElement elem) {
        CircuitNode node = nodes.computeIfAbsent(pos, k -> {
        CircuitNode newNode = new CircuitNode();
        newNode.position = pos; // 위치 저장
        return newNode;
        });
        node.connectedElements.add(elem);
    }
    public CircuitEditor() {
        setBackground(Color.WHITE);
        setPreferredSize(new Dimension(1000, 600));
        addMouseListener(this);
        addMouseMotionListener(this);
    }
    public Map<Point2D, CircuitNode> getNodes() {
        return nodes;
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
    } else {
        // 선택된 요소가 없으면 삭제 모드 활성화 후 클릭으로 삭제 처리
        deleteMode = true;
    }
}
    public CircuitAnalysisResult analyzeCircuit(double voltage) {
        boolean hasInductor = elements.stream().anyMatch(e -> e.type == ComponentType.INDUCTOR);
        boolean hasCapacitor = elements.stream().anyMatch(e -> e.type == ComponentType.CAPACITOR);
        boolean hasOpAmp = elements.stream().anyMatch(e -> e.type == ComponentType.OP_AMP);

        double R = elements.stream()
        .filter(e -> e.type == ComponentType.RESISTOR)
        .mapToDouble(e -> e.gunny)
        .sum();
        
        // OP Amp 해석 
        if(hasOpAmp) {
            analyzeOpAmps();
        }
        
        // RL, RC, RLC 해석
        if(hasInductor && !hasCapacitor) {
            double L = elements.stream()
                .filter(e -> e.type == ComponentType.INDUCTOR)
                .mapToDouble(e -> e.gunny)
                .sum();
            double tau = L / R;
            CircuitAnalysisResult result = new CircuitAnalysisResult(R, L, 0, tau, "RL");
            result.hasOpAmp = hasOpAmp;
            return result;
        } else if(hasCapacitor && !hasInductor) {
            double C = elements.stream()
                .filter(e -> e.type == ComponentType.CAPACITOR)
                .mapToDouble(e -> e.gunny)
                .sum();
            double tau = R * C;
            CircuitAnalysisResult result = new CircuitAnalysisResult(R, 0, C, tau, "RC");
            result.hasOpAmp = hasOpAmp;
            return result;
        } else if (hasInductor && hasCapacitor) {
                double L = elements.stream()
                .filter(e -> e.type == ComponentType.INDUCTOR)
                .mapToDouble(e -> e.gunny)
                .sum();
                double C = elements.stream()
                .filter(e -> e.type == ComponentType.CAPACITOR)
                .mapToDouble(e -> e.gunny)
                .sum();
                
                // 0으로 나누기 방지
                double tau = (L > 0 && R > 0) ? 1 / (R / (2 * L)) : 0.001; // 대표 시간 상수
                CircuitAnalysisResult result = new CircuitAnalysisResult(R, L, C, tau, "RLC");
                result.hasOpAmp = hasOpAmp;
                return result;
        } else {
            CircuitAnalysisResult result = new CircuitAnalysisResult(R, 0, 0, 0, null);
            result.hasOpAmp = hasOpAmp;
            return result;
        }
    }
    // 노드 전압을 저장하는 맵 (노드 ID 또는 객체를 키로 사용)
    private Map<Object, Double> nodeVoltages = new HashMap<>();
    // 노드 전압 읽기
    private double getNodeVoltage(Object node) {
        return nodeVoltages.getOrDefault(node, 0.0);
    }
    // 노드 전압 쓰기
    private void setNodeVoltage(Object node, double voltage) {
        nodeVoltages.put(node, voltage);
    }
    private void analyzeOpAmps() {
        for(CircuitElement e : elements) {
            if(e.type == ComponentType.OP_AMP) {
                analyzeOpAmp(e);
            }
        }
    }
    private void analyzeOpAmp(CircuitElement opAmp) {
        // opAmp.nonInvertingInputNode, opAmp.invertingInputNode, opAmp.outputNode 가 있다고 가정
        double vPlus = getNodeVoltage(opAmp.nonInvertingInputNode);
        double vMinus = getNodeVoltage(opAmp.invertingInputNode);
        double gain = 1e5;
        double vOut = gain * (vPlus - vMinus);
        setNodeVoltage(opAmp.outputNode, vOut);
        // 입력 전류 0 처리 등 추가 회로 방정식 반영 필요
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
    private void drawOpAmp(Graphics2D g2, Rectangle2D rect) {
        Path2D path = new Path2D.Double();
        path.moveTo(rect.getX(), rect.getY());
        path.lineTo(rect.getMaxX(), rect.getCenterY());
        path.lineTo(rect.getX(), rect.getMaxY());
        path.closePath();
        g2.setColor(Color.BLACK);
        g2.draw(path);
        // OP Amp 레이블
        g2.drawString("OP Amp", (int)rect.getCenterX() - 20, (int)rect.getCenterY());
    }
    @Override
    public void mouseClicked(MouseEvent e) {
        Point2D pos = snapToGrid(e.getPoint());
        
        // 삭제 모드 처리
        if (deleteMode) {
        for (int i = elements.size() - 1; i >= 0; i--) {
            CircuitElement elem = elements.get(i);  
            boolean hit = false;
            //전선이아닌경우: shape이 존재하고 클릭좌표주변과 교차하면 hit
            if(elem.shape != null && elem.shape.intersects(pos.getX() - 3, pos.getY() - 3, 6, 6)) {
                hit = true;
            }
            //전선인 경우: shape 대신 직접 거리 기반으로 검사
            if(elem.type == ComponentType.WIRE && elem.isNearWire(e.getPoint())) {
                hit = true;
            }
            if(hit) {
                elements.remove(i);
                repaint();
                deleteMode = false;
                return;
            }
        }
        deleteMode = false;
        }
        
        //더블클릭: gunny 편집
        if (e.getClickCount() == 2) {
            System.out.println("더블클릭 감지 at: " + pos);
            for(CircuitElement elem : elements) {
                if((elem.type == ComponentType.RESISTOR ||
                    elem.type == ComponentType.INDUCTOR ||
                    elem.type == ComponentType.CAPACITOR)
                    && elem.shape != null
                    && elem.shape.intersects(pos.getX() - 3, pos.getY() - 3, 6, 6)) {
                    editGunny(elem);
                    return;
                }
            }
        }
        //일반 클릭 처리
        else {
            Point2D snapped = snapToGrid(e.getPoint());
            CircuitElement newElement = null;
            switch(currentTool) {
                case RESISTOR:
                    newElement = new CircuitElement(ComponentType.RESISTOR, snapped);
                    break;
                case INDUCTOR:
                    newElement = new CircuitElement(ComponentType.INDUCTOR, snapped);
                    break;
                case CAPACITOR:
                    newElement = new CircuitElement(ComponentType.CAPACITOR, snapped);
                    break;
                case OP_AMP:
                    newElement = new CircuitElement(ComponentType.OP_AMP, snapped);
                    newElement.shape = new Rectangle2D.Double(snapped.getX(), snapped.getY(), 60, 40);
                    break;
                default:
                    break;
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
            updateNodes(tempWire); // 전선도 연결 노드 등록
            tempWire = null;
            repaint();
        }
    }
    private Point2D snapToGrid(Point2D p) {
        int x = ((int)p.getX() / 20) * 20;
        int y = ((int)p.getY() / 20) * 20;
        return new Point2D.Double(x, y);
    }
    
    // 값 포맷팅 메서드 - 과학적 표기법을 일반 표기법으로 변환
    private String formatValue(double value) {
        if (value >= 1000) {
            return String.format("%.0f", value);
        } else if (value >= 1) {
            return String.format("%.2f", value);
        } else if (value >= 0.01) {
            return String.format("%.4f", value);
        } else if (value >= 0.0001) {
            return String.format("%.6f", value);
        } else {
            // 매우 작은 값의 경우 μ, n, p 단위 사용
            if (value >= 1e-6) {
                return String.format("%.2fμ", value * 1e6);
            } else if (value >= 1e-9) {
                return String.format("%.2fn", value * 1e9);
            } else if (value >= 1e-12) {
                return String.format("%.2fp", value * 1e12);
            } else {
                return String.format("%.2e", value);
            }
        }
    }
    @Override public void mouseMoved(MouseEvent e) {}
    @Override public void mouseEntered(MouseEvent e) {}
    @Override public void mouseExited(MouseEvent e) {}
    private VoltageSource voltageSource = new VoltageSource(new Point2D.Double(50, 300));
    class VoltageSource {
        Point2D position;
        double voltage;

        public VoltageSource(Point2D position) {
            this.position = position;
            this.voltage = 12; // Default voltage value
        }
    }
}
class CircuitAnalysisResult {
    double R;
    double L;
    double C;
    double tau;
    String circuitType;
    List<Double> responseData;
    Map<Point2D, ParallelGroup> parallelGroups = new HashMap<>();
    boolean hasOpAmp = false;
    double alpha;
    double omega0;
    String dampingType;
    
    public Map<Point2D, ParallelGroup> detectParallels(Map<Point2D, CircuitNode> nodes) {
        parallelGroups.clear();
        nodes.values().forEach(node -> {
            if(node.connectedElements.size() > 1) {
                ParallelGroup group = new ParallelGroup();
                node.connectedElements.forEach(elem -> {
                    switch(elem.type) {
                        case RESISTOR: group.resistors.add(elem); break;
                        case INDUCTOR: group.inductors.add(elem); break;
                        case CAPACITOR: group.capacitors.add(elem); break;
                    }
                });
                parallelGroups.put(node.position, group);
            }
        });
        return parallelGroups;
    }
    
    public CircuitAnalysisResult(double R, double L, double C, double tau, String type) {
        this.R = R;
        this.L = L;
        this.C = C;
        this.tau = tau;
        this.circuitType = type;
        this.responseData = calculateResponse(tau);
        
        // RLC 회로의 경우 감쇠 특성 계산
        if ("RLC".equals(type) && L > 0 && C > 0) {
            this.alpha = R / (2 * L);
            this.omega0 = 1 / Math.sqrt(L * C);
            if (alpha > omega0) {
                this.dampingType = "과감쇠 (Overdamped)";
            } else if (Math.abs(alpha - omega0) < 1e-6) {
                this.dampingType = "임계감쇠 (Critically Damped)";
            } else {
                this.dampingType = "미감쇠 (Underdamped)";
            }
        }
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
    public double calculateEquivalentResistance(List<CircuitElement> resistors) {
        return 1.0 / resistors.stream()
            .mapToDouble(e -> 1/e.gunny)
            .sum();
    }
    
    public double calculateEquivalentInductance(List<CircuitElement> inductors) {
        return 1.0 / inductors.stream()
            .mapToDouble(e -> 1/e.gunny)
            .sum();
    }
    
    public double calculateEquivalentCapacitance(List<CircuitElement> capacitors) {
        return capacitors.stream()
            .mapToDouble(e -> e.gunny)
            .sum();
    }
}
//노드 관리 시스템
class CircuitNode {
    Point2D position;
    List<CircuitElement> connectedElements = new ArrayList<>();
}
//병렬 연결 감지 로직
class ParallelGroup {
    ArrayList<CircuitElement> resistors = new ArrayList<>();
    ArrayList<CircuitElement> inductors = new ArrayList<>();
    ArrayList<CircuitElement> capacitors = new ArrayList<>();
}
