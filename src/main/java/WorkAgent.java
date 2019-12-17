import jade.core.Agent;
import jade.core.AID;
import jade.core.behaviours.*;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import java.util.*;

public class WorkAgent extends Agent {

    private Integer[] linkedAgents;
    private float selfValue;
    private float controlVal;
    private float conformity;
    private String selfId;
    private int iter;

    @Override
    protected void setup() {
        Object[] args = getArguments();
        linkedAgents = (Integer[])args.clone();

        selfId = getAID().getLocalName();

        Random rand = new Random();
        selfValue = rand.nextInt(100);

        controlVal = 0.0f;
        conformity = 0.1f;
        iter = 0;

        System.out.println("Agent " + selfId + " is ready; current state: " + selfValue);

        //отправляет свое значение для точного вычисления мреднего
        addBehaviour(new SendToExactValueAgent());
        //отправляем PROPAGATE свои значения соседям
        addBehaviour(new SendPropagate(this, 100));
        //получаем PROPAGATE значения, считаем упрвление и, если нужно, отправляем PROPOSE, обновляя свое значение
        addBehaviour(new ReceivePropagateAndСalculateControl());
        //получаем PROPOSE с кусочком и принимаем решение в зависимости от знака управления
        addBehaviour(new ReceiveProposeAndAnswer());
        //Получаем INFORM, обновляем свое значение
        addBehaviour(new ReceiveInfAndUpdateValue());

    }

    @Override
    protected void takeDown() {
        System.out.println("Agent " + selfId + " is terminating");
    }

    private class SendToExactValueAgent extends OneShotBehaviour {

        public void action() {
            ACLMessage outMsg = new ACLMessage(ACLMessage.PROPAGATE);
            outMsg.setContent(String.valueOf(selfValue));
            outMsg.setConversationId("consensus-state");
            outMsg.addReceiver(new AID("exVal", AID.ISLOCALNAME));
            myAgent.send(outMsg);
        }
    }

    private class SendPropagate extends TickerBehaviour {

        SendPropagate(Agent a, long period) {
            super(a, period);
        }

        @Override
        protected void onTick() {
            iter++;
            if (iter % 100 == 0) {
                System.out.println(selfId + " state " + selfValue + " at " + iter + " iteration");
            }

            ACLMessage outPropagateMsg = new ACLMessage(ACLMessage.PROPAGATE);
            String msgContent = String.valueOf(selfValue);
            for (Integer linkedAgent : linkedAgents) {
                msgContent += " " + linkedAgent;
            }
            outPropagateMsg.setContent(msgContent);
            outPropagateMsg.setConversationId("consensus-state");
            outPropagateMsg.setReplyWith("consensusJobStatePpg" + System.currentTimeMillis());
            outPropagateMsg.setSender(new AID(selfId, AID.ISLOCALNAME));
            outPropagateMsg.addReceiver(new AID("env", AID.ISLOCALNAME));
            myAgent.send(outPropagateMsg);
        }
    }

    private class ReceivePropagateAndСalculateControl extends CyclicBehaviour {
        Float inVal;

        @Override
        public void action() {
            MessageTemplate mt = MessageTemplate.and(MessageTemplate.MatchPerformative(ACLMessage.PROPAGATE),
                    MessageTemplate.MatchConversationId("consensus-state"));
            ACLMessage inPropagateMsg = myAgent.receive(mt);
            if (inPropagateMsg != null) {
                inVal = Float.parseFloat(inPropagateMsg.getContent());
                controlVal += conformity * (selfValue - inVal);

                if (controlVal > 0.0f) { //т.е есть что отдать
                    ACLMessage outProposeMsg = new ACLMessage(ACLMessage.PROPOSE); //предлагаем кусочек соседям

                    String outMsgContent = String.valueOf(controlVal);
                    for (Integer linkedAgent : linkedAgents) {
                        outMsgContent += " " + String.valueOf(linkedAgent);
                    }
                    outProposeMsg.setContent(outMsgContent);
                    outProposeMsg.setConversationId("consensus-state");
                    outProposeMsg.setReplyWith("consensusJobPoolPps" + System.currentTimeMillis());
                    outProposeMsg.setSender(new AID(selfId, AID.ISLOCALNAME));
                    outProposeMsg.addReceiver(new AID("env", AID.ISLOCALNAME));

                    selfValue -= controlVal;
                    controlVal = 0.0f;
                    myAgent.send(outProposeMsg);
                }
            }
        }
    }

    private class ReceiveProposeAndAnswer extends CyclicBehaviour {

        @Override
        public void action() {
            MessageTemplate mt = MessageTemplate.and(MessageTemplate.MatchPerformative(ACLMessage.PROPOSE),
                    MessageTemplate.MatchConversationId("consensus-state"));
            ACLMessage inProposeMsg = myAgent.receive(mt);
            if (inProposeMsg != null) {
                float inVal = Float.parseFloat(inProposeMsg.getContent());
                Float newVal = controlVal + inVal;

                ACLMessage replyMsg = inProposeMsg.createReply();
                replyMsg.setConversationId("consensus-state");
                replyMsg.setSender(new AID(selfId, AID.ISLOCALNAME));
                replyMsg.addReceiver(new AID("env", AID.ISLOCALNAME));
                String msgContent;

                if (controlVal <= 0.0f) {
                    replyMsg.setPerformative(ACLMessage.ACCEPT_PROPOSAL);
                    msgContent = Math.max(newVal, 0) + " " + inProposeMsg.getSender().getLocalName();
                    replyMsg.setContent(msgContent);
                    selfValue += Math.min(-controlVal, inVal);
                    controlVal = newVal;
                } else {
                    replyMsg.setPerformative(ACLMessage.REJECT_PROPOSAL);
                    msgContent = inVal + " " + inProposeMsg.getSender().getLocalName();
                    replyMsg.setContent(msgContent);
                }
                myAgent.send(replyMsg);
            }
        }
    }

    private class ReceiveInfAndUpdateValue extends CyclicBehaviour {

        @Override
        public void action() {
            MessageTemplate mt = MessageTemplate.and(MessageTemplate.MatchPerformative(ACLMessage.INFORM),
                    MessageTemplate.MatchConversationId("consensus-state"));
            ACLMessage stateReturnMsg = myAgent.receive(mt);
            if (stateReturnMsg != null) {
                selfValue += Float.parseFloat(stateReturnMsg.getContent());
            }
        }
    }
}
