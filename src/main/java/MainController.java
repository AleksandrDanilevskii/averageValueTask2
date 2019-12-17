import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.core.Runtime;
import jade.wrapper.AgentController;
import jade.wrapper.ContainerController;
import java.util.*;

class MainController {

    private static int numberOfAgents = 10;
    private static HashMap<Integer, Integer[]> relatedAgents; //связи

    MainController() {
        relatedAgents = new HashMap<>();
        setGraph();
    }

    void initAgents() {
        Runtime rt = Runtime.instance();
        Profile p = new ProfileImpl();
        p.setParameter(Profile.MAIN_HOST, "localhost");
        p.setParameter(Profile.MAIN_PORT, "10098");
        p.setParameter(Profile.GUI, "false");
        ContainerController cc = rt.createMainContainer(p);

        try {
            //агент считает точное среднее
            AgentController ExValAgent = cc.createNewAgent("exVal",
                    "ExactValueAgent",
                    new Integer[] {numberOfAgents});
            ExValAgent.start();
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            //агент-окружение. Посредник в пересылке писем. Добавляет шум, задержки и пропажи
            AgentController EnvAgent = cc.createNewAgent("env",
                    "EnvironmentAgent",
                    null);
            EnvAgent.start();
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            //рабочие агенты
            for (int i = 0; i < numberOfAgents; i++) {
                AgentController WorkAgent = cc.createNewAgent(Integer.toString(i),
                        "WorkAgent",
                        relatedAgents.get(i));
                WorkAgent.start();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setGraph() {
        numberOfAgents = 10;
        relatedAgents.put(0, new Integer[] {1});
        relatedAgents.put(1, new Integer[] {2, 3});
        relatedAgents.put(2, new Integer[] {0, 4});
        relatedAgents.put(3, new Integer[] {4, 6});
        relatedAgents.put(4, new Integer[] {1, 5, 7});
        relatedAgents.put(5, new Integer[] {2, 8});
        relatedAgents.put(6, new Integer[] {7});
        relatedAgents.put(7, new Integer[] {3, 8});
        relatedAgents.put(8, new Integer[] {4, 9});
        relatedAgents.put(9, new Integer[] {5});
    }
}