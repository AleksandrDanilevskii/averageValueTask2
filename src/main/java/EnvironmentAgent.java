import jade.core.Agent;
import jade.core.AID;
import jade.core.behaviours.*;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import java.util.*;

public class EnvironmentAgent extends Agent {

    private String selfId;

    @Override
    protected void setup() {
        selfId = getAID().getLocalName();
        System.out.println("Agent " + selfId + " is ready");

        //принимаем PROPAGATE, добавляем шум и отправляем адресатам
        addBehaviour(new PropagateIntermediary());
        //получаем PROPOSE, +шум, делим кусок на кусочки и отправляем соседям
        addBehaviour(new ProposeIntermediary());
        //пересылаем согласие/отказ на предложение + шум
        addBehaviour(new AcceptAndRejectProposalIntermediary());
    }


    @Override
    protected void takeDown() {
        System.out.println("Agent " + selfId + " is terminating");
    }

    private class PropagateIntermediary extends CyclicBehaviour {

        public void action() {
            MessageTemplate mt = MessageTemplate.and(MessageTemplate.MatchPerformative(ACLMessage.PROPAGATE),
                    MessageTemplate.MatchConversationId("consensus-state"));
            ACLMessage inPropagateMsg = myAgent.receive(mt);
            if (inPropagateMsg != null) {
                String msgContent = inPropagateMsg.getContent();
                String[] contentList = msgContent.split(" ");
                float inValue = Float.parseFloat(contentList[0]);

                //пошумим?
                Random rand = new Random();
                float outValue = inValue + 2 * rand.nextFloat() - 1; //-1..1

                ACLMessage outPropMsg = new ACLMessage(ACLMessage.PROPAGATE);
                outPropMsg.setContent(String.valueOf(outValue));
                outPropMsg.setConversationId("consensus-state");
                outPropMsg.setReplyWith(inPropagateMsg.getReplyWith());
                outPropMsg.setSender(inPropagateMsg.getSender());
                for (int i = 1; i < contentList.length; i++) {
                    outPropMsg.addReceiver(new AID(contentList[i], AID.ISLOCALNAME));
                }
                myAgent.send(outPropMsg);
            }
        }
    }


    private class ProposeIntermediary extends CyclicBehaviour {

        private HashMap<String, HashMap<String, ArrayList<Float>>> poolMessages = new HashMap<>();

        public void action() {
            MessageTemplate mt = MessageTemplate.and(MessageTemplate.MatchPerformative(ACLMessage.PROPOSE),
                    MessageTemplate.MatchConversationId("consensus-state"));
            ACLMessage inProposeMsg = myAgent.receive(mt);
            if (inProposeMsg != null) {
                String msgContent = inProposeMsg.getContent();
                String[] contentList = msgContent.split(" ");
                String senderId = inProposeMsg.getSender().getLocalName();
                float inVal = Float.parseFloat(contentList[0]);
                int count = contentList.length - 1;

                //пошумим
                Random rand = new Random();
                float nxtNormal = 0.01f * (float)rand.nextGaussian();
                float outVal = inVal / count + nxtNormal;

                float delay = rand.nextFloat();
                float loss = rand.nextFloat();
                for (int i = 1; i < contentList.length; i++) {
                    String recipientId = contentList[i];
                    if (loss < 0.99f) {
                        if (delay < 0.99f) {
                            //все ок
                            ACLMessage outProposeMsg = new ACLMessage(ACLMessage.PROPOSE);
                            outProposeMsg.setContent(String.valueOf(outVal));
                            outProposeMsg.setConversationId("consensus-state");
                            outProposeMsg.setReplyWith(inProposeMsg.getReplyWith());
                            outProposeMsg.setSender(inProposeMsg.getSender());
                            outProposeMsg.addReceiver(new AID(recipientId, AID.ISLOCALNAME));
                            myAgent.send(outProposeMsg);

                            if (poolMessages.get(senderId) != null) {
                                HashMap<String, ArrayList<Float>> poolHolderMap = poolMessages.get(senderId);
                                if (poolHolderMap.get(recipientId) != null) {
                                    List<Float> poolMapList = poolHolderMap.get(recipientId);
                                    for (int j = 0; j < poolMapList.size(); j++) {
                                        Float valGet = poolMapList.get(j);
                                        poolMessages.get(senderId).get(recipientId).remove(valGet);

                                        outProposeMsg = new ACLMessage(ACLMessage.PROPOSE);
                                        outProposeMsg.setContent(String.valueOf(valGet));
                                        outProposeMsg.setConversationId("consensus-state");
                                        outProposeMsg.setReplyWith(inProposeMsg.getReplyWith());
                                        outProposeMsg.setSender(inProposeMsg.getSender());
                                        outProposeMsg.addReceiver(new AID(recipientId, AID.ISLOCALNAME));
                                        myAgent.send(outProposeMsg);
                                    }
                                }
                            }
                        } else {
                            //задержка. Кладем в пул
                            if (poolMessages.get(senderId) != null) {
                                if (poolMessages.get(senderId).get(recipientId) != null) {
                                    poolMessages.get(senderId).get(recipientId).add(outVal);
                                } else {
                                    poolMessages.get(senderId).put(recipientId, new ArrayList<>());
                                    poolMessages.get(senderId).get(recipientId).add(outVal);
                                }
                            } else {
                                poolMessages.put(senderId, new HashMap<>());
                                poolMessages.get(senderId).put(recipientId, new ArrayList<>());
                                poolMessages.get(senderId).get(recipientId).add(outVal);
                            }
                        }
                    }
                }

            }
        }
    }


    private class AcceptAndRejectProposalIntermediary extends CyclicBehaviour {

        public void action() {
            MessageTemplate mt = MessageTemplate.and(MessageTemplate.or(MessageTemplate.MatchPerformative(ACLMessage.ACCEPT_PROPOSAL),
                    MessageTemplate.MatchPerformative(ACLMessage.REJECT_PROPOSAL)),
                    MessageTemplate.MatchConversationId("consensus-state"));
            ACLMessage inProposalMsg = myAgent.receive(mt);
            if (inProposalMsg != null) {
                String msgContent = inProposalMsg.getContent();
                String[] contentList = msgContent.split(" ");
                float inVal = Float.parseFloat(contentList[0]);
                String recipient = contentList[1];

                //пошумим
                Random rand = new Random();
                float outVal = inVal + rand.nextFloat() - 0.5f;

                ACLMessage refillFurther = new ACLMessage(ACLMessage.INFORM);
                refillFurther.setContent(String.valueOf(outVal));
                refillFurther.setConversationId("consensus-state");
                refillFurther.setReplyWith(inProposalMsg.getReplyWith());
                refillFurther.setSender(inProposalMsg.getSender());
                refillFurther.addReceiver(new AID(recipient, AID.ISLOCALNAME));
                myAgent.send(refillFurther);
            }
        }
    }
}
