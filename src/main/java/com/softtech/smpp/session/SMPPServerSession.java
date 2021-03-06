/*
 * Licensed under the Apache License, Version 2.0 (the "License"); 
 * you may not use this file except in compliance with the License. 
 * You may obtain a copy of the License at
 * 
 *    http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 */
package com.softtech.smpp.session;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeoutException;

import com.softtech.smpp.DefaultPDUReader;
import com.softtech.smpp.DefaultPDUSender;
import com.softtech.smpp.InvalidCommandLengthException;
import com.softtech.smpp.InvalidResponseException;
import com.softtech.smpp.PDUException;
import com.softtech.smpp.PDUReader;
import com.softtech.smpp.PDUSender;
import com.softtech.smpp.PDUStringException;
import com.softtech.smpp.SMPPConstant;
import com.softtech.smpp.SynchronizedPDUSender;
import com.softtech.smpp.bean.Bind;
import com.softtech.smpp.bean.BindType;
import com.softtech.smpp.bean.CancelSm;
import com.softtech.smpp.bean.Command;
import com.softtech.smpp.bean.DataCoding;
import com.softtech.smpp.bean.DataSm;
import com.softtech.smpp.bean.ESMClass;
import com.softtech.smpp.bean.InterfaceVersion;
import com.softtech.smpp.bean.MessageState;
import com.softtech.smpp.bean.NumberingPlanIndicator;
import com.softtech.smpp.bean.OptionalParameter;
import com.softtech.smpp.bean.QuerySm;
import com.softtech.smpp.bean.RegisteredDelivery;
import com.softtech.smpp.bean.ReplaceSm;
import com.softtech.smpp.bean.SubmitMulti;
import com.softtech.smpp.bean.SubmitMultiResult;
import com.softtech.smpp.bean.SubmitSm;
import com.softtech.smpp.bean.TypeOfNumber;
import com.softtech.smpp.extra.NegativeResponseException;
import com.softtech.smpp.extra.PendingResponse;
import com.softtech.smpp.extra.ProcessRequestException;
import com.softtech.smpp.extra.ResponseTimeoutException;
import com.softtech.smpp.extra.SessionState;
import com.softtech.smpp.session.connection.Connection;
import com.softtech.smpp.util.MessageId;

/**
 * @author SUTHAR
 *
 */
public class SMPPServerSession extends AbstractSession implements ServerSession {

    private static final String MESSAGE_RECEIVER_LISTENER_IS_NULL = "Received SubmitMultiSm but MessageReceiverListener is null, returning SMPP error";
    private static final String NO_MESSAGE_RECEIVER_LISTENER_REGISTERED = "No message receiver listener registered";

    private final Connection conn;
    private final DataInputStream in;
    private final OutputStream out;

    private final PDUReader pduReader;

    private SMPPServerSessionContext sessionContext = new SMPPServerSessionContext(this);
    private final ServerResponseHandler responseHandler = new ResponseHandlerImpl();

    private ServerMessageReceiverListener messageReceiverListener;
    private ServerResponseDeliveryListener responseDeliveryListener;
    private BindRequestReceiver bindRequestReceiver = new BindRequestReceiver(responseHandler);

    public SMPPServerSession(Connection conn,
            SessionStateListener sessionStateListener,
            ServerMessageReceiverListener messageReceiverListener,
            ServerResponseDeliveryListener responseDeliveryListener,
            int pduProcessorDegree) {
        this(conn, sessionStateListener, messageReceiverListener,
                responseDeliveryListener, pduProcessorDegree,
                new SynchronizedPDUSender(new DefaultPDUSender()),
                new DefaultPDUReader());
    }

    public SMPPServerSession(Connection conn,
            SessionStateListener sessionStateListener,
            ServerMessageReceiverListener messageReceiverListener,
            ServerResponseDeliveryListener responseDeliveryListener,
            int pduProcessorDegree, PDUSender pduSender, PDUReader pduReader) {
        super(pduSender);
        this.conn = conn;
        this.messageReceiverListener = messageReceiverListener;
        this.responseDeliveryListener = responseDeliveryListener;
        this.pduReader = pduReader;
        this.in = new DataInputStream(conn.getInputStream());
        this.out = conn.getOutputStream();
        enquireLinkSender = new EnquireLinkSender();
        addSessionStateListener(new BoundStateListener());
        addSessionStateListener(sessionStateListener);
        setPduProcessorDegree(pduProcessorDegree);
        sessionContext.open();
    }

    public InetAddress getInetAddress() {
        return connection().getInetAddress();
    }

    /**
     * Wait for bind request.
     *
     * @param timeout is the timeout.
     * @return the {@link BindRequest}.
     * @throws IllegalStateException if this invocation of this method has been
     * made or invoke when state is not OPEN.
     * @throws TimeoutException if the timeout has been reach and
     * {@link SMPPServerSession} are no more valid because the connection will
     * be close automatically.
     */
    public BindRequest waitForBind(long timeout) throws IllegalStateException,
            TimeoutException {
        SessionState currentSessionState = getSessionState();
        if (currentSessionState.equals(SessionState.OPEN)) {
            new PDUReaderWorker().start();
            try {
                return bindRequestReceiver.waitForRequest(timeout);
            } catch (IllegalStateException e) {
                throw new IllegalStateException(
                        "Invocation of waitForBind() has been made", e);
            } catch (TimeoutException e) {
                close();
                throw e;
            }
        } else {
            throw new IllegalStateException(
                    "waitForBind() should be invoked on OPEN state, actual state is "
                    + currentSessionState);
        }
    }

    public void deliverShortMessage(String serviceType,
            TypeOfNumber sourceAddrTon, NumberingPlanIndicator sourceAddrNpi,
            String sourceAddr, TypeOfNumber destAddrTon,
            NumberingPlanIndicator destAddrNpi, String destinationAddr,
            ESMClass esmClass, byte protocoId, byte priorityFlag,
            RegisteredDelivery registeredDelivery, DataCoding dataCoding,
            byte[] shortMessage, OptionalParameter... optionalParameters)
            throws PDUException, ResponseTimeoutException,
            InvalidResponseException, NegativeResponseException, IOException {

        ensureReceivable("deliverShortMessage");

        DeliverSmCommandTask task = new DeliverSmCommandTask(pduSender(),
                serviceType, sourceAddrTon, sourceAddrNpi, sourceAddr,
                destAddrTon, destAddrNpi, destinationAddr, esmClass, protocoId,
                protocoId, registeredDelivery, dataCoding, shortMessage,
                optionalParameters);

        executeSendCommand(task, getTransactionTimer());
    }

    /* (non-Javadoc)
     * @see com.softtech.session.ServerSession#alertNotification(int, com.softtech.bean.TypeOfNumber, com.softtech.bean.NumberingPlanIndicator, java.lang.String, com.softtech.bean.TypeOfNumber, com.softtech.bean.NumberingPlanIndicator, java.lang.String, com.softtech.bean.OptionalParameter[])
     */
    public void alertNotification(int sequenceNumber,
            TypeOfNumber sourceAddrTon, NumberingPlanIndicator sourceAddrNpi,
            String sourceAddr, TypeOfNumber esmeAddrTon,
            NumberingPlanIndicator esmeAddrNpi, String esmeAddr,
            OptionalParameter... optionalParameters) throws PDUStringException,
            IOException {

        ensureReceivable("alertNotification");

        pduSender().sendAlertNotification(connection().getOutputStream(),
                sequenceNumber, sourceAddrTon.value(), sourceAddrNpi.value(),
                sourceAddr, esmeAddrTon.value(), esmeAddrNpi.value(), esmeAddr,
                optionalParameters);
    }

    private MessageId fireAcceptSubmitSm(SubmitSm submitSm) throws ProcessRequestException {
        if (messageReceiverListener != null) {
            return messageReceiverListener.onAcceptSubmitSm(submitSm, this);
        }
        throw new ProcessRequestException(NO_MESSAGE_RECEIVER_LISTENER_REGISTERED,
                SMPPConstant.STAT_ESME_RX_R_APPN);
    }

    private SubmitMultiResult fireAcceptSubmitMulti(SubmitMulti submitMulti) throws ProcessRequestException {
        if (messageReceiverListener != null) {
            return messageReceiverListener.onAcceptSubmitMulti(submitMulti, this);
        }
        throw new ProcessRequestException(NO_MESSAGE_RECEIVER_LISTENER_REGISTERED,
                SMPPConstant.STAT_ESME_RX_R_APPN);
    }

    private QuerySmResult fireAcceptQuerySm(QuerySm querySm) throws ProcessRequestException {
        if (messageReceiverListener != null) {
            return messageReceiverListener.onAcceptQuerySm(querySm, this);
        }
        throw new ProcessRequestException(NO_MESSAGE_RECEIVER_LISTENER_REGISTERED,
                SMPPConstant.STAT_ESME_RX_R_APPN);
    }

    private void fireAcceptReplaceSm(ReplaceSm replaceSm) throws ProcessRequestException {
        if (messageReceiverListener != null) {
            messageReceiverListener.onAcceptReplaceSm(replaceSm, this);
        } else {
            throw new ProcessRequestException(NO_MESSAGE_RECEIVER_LISTENER_REGISTERED,
                    SMPPConstant.STAT_ESME_RX_R_APPN);
        }
    }

    private void fireAcceptCancelSm(CancelSm cancelSm) throws ProcessRequestException {
        if (messageReceiverListener != null) {
            messageReceiverListener.onAcceptCancelSm(cancelSm, this);
        } else {
            throw new ProcessRequestException(NO_MESSAGE_RECEIVER_LISTENER_REGISTERED,
                    SMPPConstant.STAT_ESME_RX_R_APPN);
        }
    }

    private void fireSubmitSmRespSent(MessageId messageId) {
        if (responseDeliveryListener != null) {
            responseDeliveryListener.onSubmitSmRespSent(messageId, this);
        }
    }

    private void fireSubmitSmRespFailed(MessageId messageId, Exception cause) {
        if (responseDeliveryListener != null) {
            responseDeliveryListener.onSubmitSmRespError(messageId, cause,
                    this);
        }
    }

    private void fireSubmitMultiRespSent(SubmitMultiResult submitMultiResult) {
        if (responseDeliveryListener != null) {
            responseDeliveryListener.onSubmitMultiRespSent(
                    submitMultiResult, this);
        }
    }

    private void fireSubmitMultiRespSentError(
            SubmitMultiResult submitMultiResult, Exception cause) {
        if (responseDeliveryListener != null) {
            responseDeliveryListener.onSubmitMultiRespError(
                    submitMultiResult, cause, this);
        }
    }

    @Override
    protected Connection connection() {
        return conn;
    }

    @Override
    protected AbstractSessionContext sessionContext() {
        return sessionContext;
    }

    @Override
    protected GenericMessageReceiverListener messageReceiverListener() {
        return messageReceiverListener;
    }

    public ServerMessageReceiverListener getMessageReceiverListener() {
        return messageReceiverListener;
    }

    public void setMessageReceiverListener(
            ServerMessageReceiverListener messageReceiverListener) {
        this.messageReceiverListener = messageReceiverListener;
    }

    public void setResponseDeliveryListener(
            ServerResponseDeliveryListener responseDeliveryListener) {
        this.responseDeliveryListener = responseDeliveryListener;
    }

    private class ResponseHandlerImpl implements ServerResponseHandler {

        public PendingResponse<Command> removeSentItem(int sequenceNumber) {
            return removePendingResponse(sequenceNumber);
        }

        public void notifyUnbonded() {
            sessionContext.unbound();
        }

        public void sendEnquireLinkResp(int sequenceNumber) throws IOException {
            pduSender().sendEnquireLinkResp(out, sequenceNumber);
        }

        public void sendGenerickNack(int commandStatus, int sequenceNumber)
                throws IOException {
            pduSender().sendGenericNack(out, commandStatus, sequenceNumber);
        }

        public void sendNegativeResponse(int originalCommandId,
                int commandStatus, int sequenceNumber) throws IOException {
            pduSender().sendHeader(out, originalCommandId | SMPPConstant.MASK_CID_RESP, commandStatus, sequenceNumber);
        }

        public void sendUnbindResp(int sequenceNumber) throws IOException {
            pduSender().sendUnbindResp(out, SMPPConstant.STAT_ESME_ROK, sequenceNumber);
        }

        public void sendBindResp(String systemId, InterfaceVersion interfaceVersion, BindType bindType, int sequenceNumber) throws IOException {
            sessionContext.bound(bindType);
            try {
                pduSender().sendBindResp(out, bindType.responseCommandId(), sequenceNumber, systemId, interfaceVersion);
            } catch (PDUStringException e) {
                // TODO SUTHAR: we have double checking when accept the bind request
            }
        }

        public void processBind(Bind bind) {
            SMPPServerSession.this.bindRequestReceiver.notifyAcceptBind(bind);
        }

        public MessageId processSubmitSm(SubmitSm submitSm)
                throws ProcessRequestException {
            try {
                MessageId messageId = fireAcceptSubmitSm(submitSm);
                if (messageId == null) {
                    String msg = "Invalid message_id, shouldn't null value. " + ServerMessageReceiverListener.class + "#onAcceptSubmitSm(SubmitSm) return null value";
                    throw new ProcessRequestException(msg, SMPPConstant.STAT_ESME_RX_R_APPN);
                }
                return messageId;
            } catch (ProcessRequestException e) {
                throw e;
            } catch (Exception e) {
                String msg = "Invalid runtime exception thrown when processing SubmitSm";
                throw new ProcessRequestException(msg, SMPPConstant.STAT_ESME_RSYSERR);
            }
        }

        public void sendSubmitSmResponse(MessageId messageId, int sequenceNumber)
                throws IOException {
            try {
                pduSender().sendSubmitSmResp(out, sequenceNumber,
                        messageId.getValue());
                fireSubmitSmRespSent(messageId);
            } catch (PDUStringException e) {
                /*
                 * There should be no PDUStringException thrown since creation
                 * of MessageId should be save.
                 */
                fireSubmitSmRespFailed(messageId, e);
            } catch (IOException e) {
                fireSubmitSmRespFailed(messageId, e);
                throw e;
            } catch (RuntimeException e) {
                fireSubmitSmRespFailed(messageId, e);
                throw e;
            }
        }

        public SubmitMultiResult processSubmitMulti(SubmitMulti submitMulti)
                throws ProcessRequestException {
            try {
                return fireAcceptSubmitMulti(submitMulti);
            } catch (Exception e) {
                String msg = "Invalid runtime exception thrown when processing SubmitMultiSm";
                throw new ProcessRequestException(msg, SMPPConstant.STAT_ESME_RSYSERR);
            }
        }

        public void sendSubmitMultiResponse(
                SubmitMultiResult submitMultiResult, int sequenceNumber)
                throws IOException {
            try {
                pduSender().sendSubmitMultiResp(out, sequenceNumber,
                        submitMultiResult.getMessageId(),
                        submitMultiResult.getUnsuccessDeliveries());
                fireSubmitMultiRespSent(submitMultiResult);
            } catch (PDUStringException e) {
                /*
                 * There should be no PDUStringException thrown since creation
                 * of the response parameter has been validated.
                 */
                fireSubmitMultiRespSentError(submitMultiResult, e);
            } catch (IOException e) {
                fireSubmitMultiRespSentError(submitMultiResult, e);
                throw e;
            } catch (RuntimeException e) {
                fireSubmitMultiRespSentError(submitMultiResult, e);
                throw e;
            }
        }

        public QuerySmResult processQuerySm(QuerySm querySm)
                throws ProcessRequestException {
            try {
                return fireAcceptQuerySm(querySm);
            } catch (Exception e) {
                String msg = "Invalid runtime exception thrown when processing QuerySm";
                throw new ProcessRequestException(msg, SMPPConstant.STAT_ESME_RSYSERR);
            }
        }

        public void sendQuerySmResp(String messageId, String finalDate,
                MessageState messageState, byte errorCode, int sequenceNumber) throws IOException {
            try {
                pduSender().sendQuerySmResp(out, sequenceNumber, messageId,
                        finalDate, messageState, errorCode);
            } catch (PDUStringException e) {
                /*
                 * There should be no PDUStringException thrown since creation
                 * of parsed messageId has been validated.
                 */
            }
        }

        public DataSmResult processDataSm(DataSm dataSm)
                throws ProcessRequestException {
            try {
                return fireAcceptDataSm(dataSm);
            } catch (Exception e) {
                String msg = "Invalid runtime exception thrown when processing DataSm";
                throw new ProcessRequestException(msg, SMPPConstant.STAT_ESME_RSYSERR);
            }
        }

        // TODO uudashr: we can generalize this method 
        public void sendDataSmResp(DataSmResult dataSmResult, int sequenceNumber)
                throws IOException {
            try {
                pduSender().sendDataSmResp(out, sequenceNumber,
                        dataSmResult.getMessageId(),
                        dataSmResult.getOptionalParameters());
            } catch (PDUStringException e) {
                /*
                 * There should be no PDUStringException thrown since creation
                 * of MessageId should be save.
                 */
            }
        }

        public void processCancelSm(CancelSm cancelSm)
                throws ProcessRequestException {
            try {
                fireAcceptCancelSm(cancelSm);
            } catch (Exception e) {
                String msg = "Invalid runtime exception thrown when processing CancelSm";
                throw new ProcessRequestException(msg, SMPPConstant.STAT_ESME_RSYSERR);
            }
        }

        public void sendCancelSmResp(int sequenceNumber) throws IOException {
            pduSender().sendCancelSmResp(out, sequenceNumber);
        }

        public void processReplaceSm(ReplaceSm replaceSm)
                throws ProcessRequestException {
            try {
                fireAcceptReplaceSm(replaceSm);
            } catch (Exception e) {
                String msg = "Invalid runtime exception thrown when processing ReplaceSm";
                throw new ProcessRequestException(msg, SMPPConstant.STAT_ESME_RSYSERR);
            }
        }

        public void sendReplaceSmResp(int sequenceNumber) throws IOException {
            pduSender().sendReplaceSmResp(out, sequenceNumber);
        }
    }

    private class PDUReaderWorker extends Thread {

        private ExecutorService executorService = Executors.newFixedThreadPool(getPduProcessorDegree());
        private Runnable onIOExceptionTask = new Runnable() {
            @Override
            public void run() {
                close();
            }
        };

        @Override
        public void run() {
            while (isReadPdu()) {
                readPDU();
            }
            close();
            executorService.shutdown();
        }

        private void readPDU() {
            try {
                Command pduHeader = pduReader.readPDUHeader(in);
                byte[] pdu = pduReader.readPDU(in, pduHeader);

                PDUProcessServerTask task = new PDUProcessServerTask(pduHeader,
                        pdu, sessionContext.getStateProcessor(),
                        sessionContext, responseHandler, onIOExceptionTask);
                executorService.execute(task);
            } catch (InvalidCommandLengthException e) {
                try {
                    pduSender().sendGenericNack(out, SMPPConstant.STAT_ESME_RINVCMDLEN, 0);
                } catch (IOException ee) {
                }
                unbindAndClose();
            } catch (SocketTimeoutException e) {
                notifyNoActivity();
            } catch (IOException e) {
                close();
            } catch (RuntimeException e) {
                unbindAndClose();
            }
        }

        /**
         * Notify for no activity.
         */
        private void notifyNoActivity() {
            enquireLinkSender.enquireLink();
        }
    }

    private class BoundStateListener implements SessionStateListener {

        public void onStateChange(SessionState newState, SessionState oldState,
                Session source) {
            if (newState.isBound()) {
                enquireLinkSender.start();
            }
        }
    }
}
