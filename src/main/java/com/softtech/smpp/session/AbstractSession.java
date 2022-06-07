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

import java.io.IOException;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import com.softtech.smpp.InvalidResponseException;
import com.softtech.smpp.PDUException;
import com.softtech.smpp.PDUSender;
import com.softtech.smpp.SMPPConstant;
import com.softtech.smpp.bean.Command;
import com.softtech.smpp.bean.DataCoding;
import com.softtech.smpp.bean.DataSm;
import com.softtech.smpp.bean.DataSmResp;
import com.softtech.smpp.bean.ESMClass;
import com.softtech.smpp.bean.NumberingPlanIndicator;
import com.softtech.smpp.bean.OptionalParameter;
import com.softtech.smpp.bean.RegisteredDelivery;
import com.softtech.smpp.bean.TypeOfNumber;
import com.softtech.smpp.extra.NegativeResponseException;
import com.softtech.smpp.extra.PendingResponse;
import com.softtech.smpp.extra.ProcessRequestException;
import com.softtech.smpp.extra.ResponseTimeoutException;
import com.softtech.smpp.extra.SessionState;
import com.softtech.smpp.session.connection.Connection;
import com.softtech.smpp.util.IntUtil;
import com.softtech.smpp.util.Sequence;

/**
 * @author SUTHAR
 *
 */
public abstract class AbstractSession implements Session {

    private static final Random random = new Random();

    private final Map<Integer, PendingResponse<Command>> pendingResponse = new ConcurrentHashMap<Integer, PendingResponse<Command>>();
    private final Sequence sequence = new Sequence(1);
    private final PDUSender pduSender;
    private int pduProcessorDegree = 3;

    private String sessionId = generateSessionId();
    private int enquireLinkTimer = 5000;
    private long transactionTimer = 6000;

    protected EnquireLinkSender enquireLinkSender;

    public AbstractSession(PDUSender pduSender) {
        this.pduSender = pduSender;
    }

    protected abstract AbstractSessionContext sessionContext();

    protected abstract Connection connection();

    protected abstract GenericMessageReceiverListener messageReceiverListener();

    protected PDUSender pduSender() {
        return pduSender;
    }

    protected Sequence sequence() {
        return sequence;
    }

    protected PendingResponse<Command> removePendingResponse(int sequenceNumber) {
        return pendingResponse.remove(sequenceNumber);
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setEnquireLinkTimer(int enquireLinkTimer) {
        if (sessionContext().getSessionState().isBound()) {
            try {
                connection().setSoTimeout(enquireLinkTimer);
            } catch (IOException e) {
            }
        }
        this.enquireLinkTimer = enquireLinkTimer;
    }

    public int getEnquireLinkTimer() {
        return enquireLinkTimer;
    }

    public void setTransactionTimer(long transactionTimer) {
        this.transactionTimer = transactionTimer;
    }

    public long getTransactionTimer() {
        return transactionTimer;
    }

    public SessionState getSessionState() {
        return sessionContext().getSessionState();
    }

    protected synchronized boolean isReadPdu() {
        SessionState sessionState = getSessionState();
        return sessionState.isBound() || sessionState.equals(SessionState.OPEN) || sessionState.equals(SessionState.OUTBOUND);
    }

    public void addSessionStateListener(SessionStateListener listener) {
        if (listener != null) {
            sessionContext().addSessionStateListener(listener);
        }
    }

    public void removeSessionStateListener(SessionStateListener listener) {
        sessionContext().removeSessionStateListener(listener);
    }

    public long getLastActivityTimestamp() {
        return sessionContext().getLastActivityTimestamp();
    }

    /**
     * Set total thread can read PDU and process it in parallel. It's defaulted
     * to 3.
     *
     * @param pduProcessorDegree is the total thread can handle read and process
     * PDU in parallel.
     * @throws IllegalStateException if the PDU Reader has been started.
     */
    public void setPduProcessorDegree(int pduProcessorDegree) throws IllegalStateException {
        if (!getSessionState().equals(SessionState.CLOSED)) {
            throw new IllegalStateException(
                    "Cannot set PDU processor degree since the PDU dispatcher thread already created");
        }
        this.pduProcessorDegree = pduProcessorDegree;
    }

    /**
     * Get the total of thread that can handle read and process PDU in parallel.
     *
     * @return the total of thread that can handle read and process PDU in
     * parallel.
     */
    public int getPduProcessorDegree() {
        return pduProcessorDegree;
    }

    /**
     * Send the data_sm command.
     *
     * @param serviceType is the service_type parameter.
     * @param sourceAddrTon is the source_addr_ton parameter.
     * @param sourceAddrNpi is the source_addr_npi parameter.
     * @param sourceAddr is the source_addr parameter.
     * @param destAddrTon is the dest_addr_ton parameter.
     * @param destAddrNpi is the dest_addr_npi parameter.
     * @param destinationAddr is the destination_addr parameter.
     * @param esmClass is the esm_class parameter.
     * @param registeredDelivery is the registered_delivery parameter.
     * @param dataCoding is the data_coding parameter.
     * @param optionalParameters is the optional parameters.
     * @return the result of data_sm (data_sm_resp).
     * @throws PDUException if there is an invalid PDU parameter found.
     * @throws ResponseTimeoutException if the response take time too long.
     * @throws InvalidResponseException if the response is invalid.
     * @throws NegativeResponseException if the response return NON-OK
     * command_status.
     * @throws IOException if there is an IO error found.
     */
    public DataSmResult dataShortMessage(String serviceType,
            TypeOfNumber sourceAddrTon, NumberingPlanIndicator sourceAddrNpi,
            String sourceAddr, TypeOfNumber destAddrTon,
            NumberingPlanIndicator destAddrNpi, String destinationAddr,
            ESMClass esmClass, RegisteredDelivery registeredDelivery,
            DataCoding dataCoding, OptionalParameter... optionalParameters)
            throws PDUException, ResponseTimeoutException,
            InvalidResponseException, NegativeResponseException, IOException {

        DataSmCommandTask task = new DataSmCommandTask(pduSender,
                serviceType, sourceAddrTon, sourceAddrNpi, sourceAddr,
                destAddrTon, destAddrNpi, destinationAddr, esmClass,
                registeredDelivery, dataCoding, optionalParameters);

        DataSmResp resp = (DataSmResp) executeSendCommand(task, getTransactionTimer());

        return new DataSmResult(resp.getMessageId(), resp.getOptionalParameters());
    }

    public void close() {
        SessionContext ctx = sessionContext();
        SessionState sessionState = ctx.getSessionState();
        if (!sessionState.equals(SessionState.CLOSED)) {
            try {
                connection().close();
            } catch (IOException e) {
            }
        }

        // Make sure the enquireLinkThread doesn't wait for itself
        if (Thread.currentThread() != enquireLinkSender) {
            if (enquireLinkSender != null && enquireLinkSender.isAlive()) {
                try {
                    enquireLinkSender.interrupt();
                    enquireLinkSender.join();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }

        if (!sessionState.equals(SessionState.CLOSED)) {
            ctx.close();
        }
    }

    /**
     * Validate the response, the command_status should be 0 otherwise will
     * throw {@link NegativeResponseException}.
     *
     * @param response is the response.
     * @throws NegativeResponseException if the command_status value is not
     * zero.
     */
    private static void validateResponse(final Command response) throws NegativeResponseException {
        if (response.getCommandStatus() != SMPPConstant.STAT_ESME_ROK) {
            throw new NegativeResponseException(response.getCommandStatus());
        }
    }

    protected DataSmResult fireAcceptDataSm(final DataSm dataSm) throws ProcessRequestException {
        GenericMessageReceiverListener messageReceiverListener = messageReceiverListener();
        if (messageReceiverListener != null) {
            return messageReceiverListener.onAcceptDataSm(dataSm, this);
        } else {
            throw new ProcessRequestException("MessageReceiverListener hasn't been set yet", SMPPConstant.STAT_ESME_RX_R_APPN);
        }
    }

    /**
     * Execute send command command task.
     *
     * @param task is the task.
     * @param timeout is the timeout in millisecond.
     * @return the command response.
     * @throws PDUException if there is invalid PDU parameter found.
     * @throws ResponseTimeoutException if the response has reach it timeout.
     * @throws InvalidResponseException if invalid response found.
     * @throws NegativeResponseException if the negative response found.
     * @throws IOException if there is an IO error found.
     */
    protected Command executeSendCommand(SendCommandTask task, long timeout)
            throws PDUException, ResponseTimeoutException,
            InvalidResponseException, NegativeResponseException, IOException {

        int seqNum = sequence.nextValue();
        PendingResponse<Command> pendingResp = new PendingResponse<Command>(timeout);
        pendingResponse.put(seqNum, pendingResp);
        try {
            task.executeTask(connection().getOutputStream(), seqNum);
        } catch (IOException e) {

            if ("enquire_link".equals(task.getCommandName())) {
            } else {
                pendingResponse.remove(seqNum);
                close();
                throw e;
            }
        }

        try {
            pendingResp.waitDone();
        } catch (ResponseTimeoutException e) {
            pendingResponse.remove(seqNum);
            throw new ResponseTimeoutException("No response after waiting for "
                    + timeout + " millis when executing "
                    + task.getCommandName() + " with sessionId " + sessionId
                    + " and sequenceNumber " + seqNum, e);
        } catch (InvalidResponseException e) {
            pendingResponse.remove(seqNum);
            throw e;
        }

        Command resp = pendingResp.getResponse();
        validateResponse(resp);
        return resp;
    }

    /**
     * Execute send command command task.
     *
     * @param task is the task.
     * @throws PDUException if there is invalid PDU parameter found.
     * @throws IOException if there is an IO error found.
     */
    protected void executeSendCommandWithNoResponse(SendCommandTask task)
            throws PDUException, IOException {

        int seqNum = sequence.nextValue();
        try {
            task.executeTask(connection().getOutputStream(), seqNum);
        } catch (IOException e) {
            close();
            throw e;
        }
    }

    private synchronized static final String generateSessionId() {
        return IntUtil.toHexString(random.nextInt());
    }

    /**
     * Ensure we have proper link.
     *
     * @throws ResponseTimeoutException if there is no valid response after
     * defined millisecond.
     * @throws InvalidResponseException if there is invalid response found.
     * @throws IOException if there is an IO error found.
     */
    protected void sendEnquireLink() throws ResponseTimeoutException, InvalidResponseException, IOException {
        EnquireLinkCommandTask task = new EnquireLinkCommandTask(pduSender);
        try {
            executeSendCommand(task, getTransactionTimer());
        } catch (PDUException e) {
            // should never happen, since it doesn't have any String parameter.
        } catch (NegativeResponseException e) {
            // the command_status of the response should be always 0
        }
    }

    public void sendOutbind(String systemId, String password) throws IOException {
        if (sessionContext().getSessionState().equals(SessionState.CLOSED)) {
            throw new IOException("Session " + sessionId + " is closed");
        }

        OutbindCommandTask task = new OutbindCommandTask(pduSender, systemId, password);

        try {
            executeSendCommandWithNoResponse(task);
        } catch (PDUException e) {
            // exception should be never caught since we didn't send any string parameter.
        }
    }

    public void unbind() throws ResponseTimeoutException,
            InvalidResponseException, IOException {
        if (sessionContext().getSessionState().equals(SessionState.CLOSED)) {
            throw new IOException("Session " + sessionId + " is closed");
        }

        UnbindCommandTask task = new UnbindCommandTask(pduSender);

        try {
            executeSendCommand(task, transactionTimer);
        } catch (PDUException e) {
            // exception should be never caught since we didn't send any string parameter.
        } catch (NegativeResponseException e) {
            // ignore the negative response
        }
    }

    public void unbindAndClose() {
        if (sessionContext().getSessionState().isBound()) {
            try {
                unbind();
            } catch (ResponseTimeoutException e) {
            } catch (InvalidResponseException e) {
            } catch (IOException e) {
            }
        }
        close();
    }

    /**
     * Ensure the session is receivable. If the session not receivable then an
     * exception thrown.
     *
     * @param activityName is the activity name.
     * @throws IOException if the session not receivable.
     */
    protected void ensureReceivable(String activityName) throws IOException {
        // TODO SUTHAR: do we have to use another exception for this checking?
        SessionState currentState = getSessionState();
        if (!currentState.isReceivable()) {
            throw new IOException("Cannot " + activityName + " while session " + sessionId + " in state " + currentState);
        }
    }

    /**
     * Ensure the session is transmittable. If the session not transmittable
     * then an exception thrown.
     *
     * @param activityName is the activity name.
     * @throws IOException if the session not transmittable.
     */
    protected void ensureTransmittable(String activityName) throws IOException {
        ensureTransmittable(activityName, false);
    }

    /**
     * Ensure the session is transmittable. If the session not transmittable
     * then an exception thrown.
     *
     * @param activityName is the activity name.
     * @param only set to <tt>true</tt> if you want to ensure transmittable only
     * (transceive will not pass), otherwise set to <tt>false</tt>.
     * @throws IOException if the session not transmittable (by considering the
     * <code>only</code> parameter).
     */
    protected void ensureTransmittable(String activityName, boolean only) throws IOException {
        // TODO SUTHAR: do we have to use another exception for this checking?
        SessionState currentState = getSessionState();
        if (!currentState.isTransmittable() || (only && currentState.isReceivable())) {
            throw new IOException("Cannot " + activityName + " while session " + sessionId + " in state " + currentState);
        }
    }

    protected class EnquireLinkSender extends Thread {

        private final AtomicBoolean sendingEnquireLink = new AtomicBoolean(false);

        public EnquireLinkSender() {
            super("EnquireLinkSender: " + AbstractSession.this);
        }

        @Override
        public void run() {
            while (isReadPdu()) {
                while (!sendingEnquireLink.compareAndSet(true, false) && !Thread.currentThread().isInterrupted() && isReadPdu()) {
                    synchronized (sendingEnquireLink) {
                        try {
                            sendingEnquireLink.wait(500);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            break;
                        }
                    }
                }
                if (Thread.currentThread().isInterrupted() || !isReadPdu()) {
                    break;
                }
                try {
                    sendEnquireLink();
                } catch (ResponseTimeoutException e) {
                    close();
                } catch (InvalidResponseException e) {
                    // lets unbind gracefully
                    unbindAndClose();
                } catch (IOException e) {
                    close();
                }
            }
        }

        /**
         * This method will send enquire link asynchronously.
         */
        public void enquireLink() {
            if (sendingEnquireLink.compareAndSet(false, true)) {
                synchronized (sendingEnquireLink) {
                    sendingEnquireLink.notify();
                }
            } else {
            }
        }
    }
}
