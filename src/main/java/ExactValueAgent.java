import jade.core.Agent;
import jade.core.behaviours.*;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

public class ExactValueAgent extends Agent {

    private String selfId;
    private int numberOfAgents;
    private int iter;
    private Float stateVal;
    private Float totalVal;

    @Override
    protected void setup() {
        selfId = getAID().getLocalName();
        System.out.println("Agent " + selfId + " is ready");

        Object[] args = getArguments();
        Integer[] argsInts = (Integer[])args.clone();
        numberOfAgents = argsInts[0];
        iter = 0;
        stateVal = 0.0f;
        totalVal = 0.0f;

        addBehaviour(new AverageСalculation());
    }


    @Override
    protected void takeDown() {
        System.out.println("Agent " + selfId + " is terminating");
    }


    private class AverageСalculation extends Behaviour {

        public void action() {
            MessageTemplate mt = MessageTemplate.and(MessageTemplate.MatchPerformative(ACLMessage.PROPAGATE),
                    MessageTemplate.MatchConversationId("consensus-state"));
            ACLMessage stateMsg = myAgent.receive(mt);
            if (stateMsg != null) {
                stateVal = Float.parseFloat(stateMsg.getContent());
                totalVal += stateVal;
                iter++;
            }
        }

        public boolean done() {
            if (iter >= numberOfAgents) {
                totalVal /= numberOfAgents;
                System.out.println("True mean: " + totalVal);
                return true;
            } else {
                return false;
            }
        }
    }
}
