import javax.swing.*;
import java.awt.*;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class CircuitDesigner extends JFrame {
    private CircuitEditor editor;
    private JPanel controlPanel;
    private JButton resistorBtn, inductorBtn, capacitorBtn, wireBtn,OP_AMPBtn, voltageSourceBtn, deleteBtn, solveBtn;
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
        voltageSourceBtn = new JButton("전압원 추가");
        deleteBtn = new JButton("삭제");
        solveBtn = new JButton("회로 해석");
        voltageField = new JTextField("12", 5);

        resistorBtn.addActionListener(e -> editor.setTool(CircuitTool.RESISTOR));
        inductorBtn.addActionListener(e -> editor.setTool(CircuitTool.INDUCTOR));
        capacitorBtn.addActionListener(e -> editor.setTool(CircuitTool.CAPACITOR));
        wireBtn.addActionListener(e -> editor.setTool(CircuitTool.WIRE));
        OP_AMPBtn.addActionListener(e -> editor.setTool(CircuitTool.OP_AMP));
        voltageSourceBtn.addActionListener(e -> editor.setTool(CircuitTool.VOLTAGE_SOURCE));
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
        controlPanel.add(voltageSourceBtn);
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
            
            // 전압원 정보 표시
            boolean hasVoltageSource = editor.getElementsSnapshot().stream()
                .anyMatch(e -> e.type == ComponentType.VOLTAGE_SOURCE);
            if (hasVoltageSource) {
                double sourceVoltage = editor.getElementsSnapshot().stream()
                    .filter(e -> e.type == ComponentType.VOLTAGE_SOURCE)
                    .mapToDouble(e -> e.gunny)
                    .findFirst()
                    .orElse(voltage);
                sb.append(String.format("전압원 전압: %.1f V\n", sourceVoltage));
            } else {
                sb.append(String.format("입력 전압: %.1f V\n", voltage));
            }
            if (result.circuitType != null && result.circuitType.contains("RL"))
                sb.append(String.format("총 등가 인덕턴스: %s H\n", formatValue(result.L)));
            if (result.circuitType != null && result.circuitType.contains("RC"))
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
        Map<Point2D, ParallelGroup> parallelGroups = editor.computeParallelGroups();
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
