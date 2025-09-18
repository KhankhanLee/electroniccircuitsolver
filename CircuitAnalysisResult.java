import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
                        case WIRE: // WIRE는 병렬 그룹에 포함하지 않음
                        case OP_AMP: // OP_AMP는 병렬 그룹에 포함하지 않음
                        default: break;
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
