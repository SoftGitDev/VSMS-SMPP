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
import java.util.HashMap;
import java.util.Map;

import com.softtech.smpp.InvalidResponseException;
import com.softtech.smpp.PDUException;
import com.softtech.smpp.PDUSender;
import com.softtech.smpp.PDUStringException;
import com.softtech.smpp.SMPPConstant;
import com.softtech.smpp.bean.Command;
import com.softtech.smpp.bean.DataCoding;
import com.softtech.smpp.bean.DataSmResp;
import com.softtech.smpp.bean.ESMClass;
import com.softtech.smpp.bean.NumberingPlanIndicator;
import com.softtech.smpp.bean.OptionalParameter;
import com.softtech.smpp.bean.RegisteredDelivery;
import com.softtech.smpp.bean.TypeOfNumber;
import com.softtech.smpp.extra.NegativeResponseException;
import com.softtech.smpp.extra.PendingResponse;
import com.softtech.smpp.extra.ResponseTimeoutException;
import com.softtech.smpp.session.connection.Connection;
import com.softtech.smpp.util.Sequence;

/**
 * @author SUTHAR
 *
 */
public abstract class AbstractSMPPOperation implements SMPPOperation {
    private final Map<Integer, PendingResponse<Command>> pendingResponse = new HashMap<Integer, PendingResponse<Command>>();
    private final Sequence sequence = new Sequence(1);
    private final PDUSender pduSender;
    private final Connection connection;
    private long transactionTimer = 2000;

    public AbstractSMPPOperation(Connection connection, PDUSender pduSender) {
        this.connection = connection;
        this.pduSender = pduSender;
    }

    protected PDUSender pduSender() {
        return pduSender;
    }

    protected Connection connection() {
        return connection;
    }

    public void setTransactionTimer(long transactionTimer) {
        this.transactionTimer = transactionTimer;
    }

    public long getTransactionTimer() {
        return transactionTimer;
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
            pendingResponse.remove(seqNum);
            throw e;
        }

        try {
            pendingResp.waitDone();
        } catch (ResponseTimeoutException e) {
            pendingResponse.remove(seqNum);
             throw e;
        } catch (InvalidResponseException e) {
            pendingResponse.remove(seqNum);
            throw e;
        }

        Command resp = pendingResp.getResponse();
        validateResponse(resp);
        return resp;

    }

    /**
     * Validate the response, the command_status should be 0 otherwise will
     * throw {@link NegativeResponseException}.
     *
     * @param response is the response.
     * @throws NegativeResponseException if the command_status value is not zero.
     */
    private static void validateResponse(Command response) throws NegativeResponseException {
        if (response.getCommandStatus() != SMPPConstant.STAT_ESME_ROK) {
            throw new NegativeResponseException(response.getCommandStatus());
        }
    }

    public void unbind() throws ResponseTimeoutException, InvalidResponseException, IOException {
        UnbindCommandTask task = new UnbindCommandTask(pduSender);

        try {
            executeSendCommand(task, transactionTimer);
        } catch (PDUException e) {
            // exception should be never caught since we didn't send any string parameter.
        } catch (NegativeResponseException e) {
            // ignore the negative response
        }
    }

    public void unbindResp(int sequenceNumber) throws IOException {
        pduSender.sendUnbindResp(connection().getOutputStream(), SMPPConstant.STAT_ESME_ROK, sequenceNumber);
    }

    public DataSmResult dataSm(String serviceType, TypeOfNumber sourceAddrTon,
            NumberingPlanIndicator sourceAddrNpi, String sourceAddr,
            TypeOfNumber destAddrTon, NumberingPlanIndicator destAddrNpi,
            String destinationAddr, ESMClass esmClass,
            RegisteredDelivery registeredDelivery, DataCoding dataCoding,
            OptionalParameter... optionalParameters) throws PDUException,
            ResponseTimeoutException, InvalidResponseException,
            NegativeResponseException, IOException {

        DataSmCommandTask task = new DataSmCommandTask(pduSender,
                serviceType, sourceAddrTon, sourceAddrNpi, sourceAddr,
                destAddrTon, destAddrNpi, destinationAddr, esmClass,
                registeredDelivery, dataCoding, optionalParameters);

        DataSmResp resp = (DataSmResp)executeSendCommand(task, getTransactionTimer());

        return new DataSmResult(resp.getMessageId(), resp.getOptionalParameters());
    }

    public void dataSmResp(int sequenceNumber, String messageId,
            OptionalParameter... optionalParameters) throws PDUStringException,
            IOException {
        pduSender.sendDataSmResp(connection().getOutputStream(), sequenceNumber, messageId, optionalParameters);
    }

    public void enquireLink() throws ResponseTimeoutException, InvalidResponseException, IOException {
        EnquireLinkCommandTask task = new EnquireLinkCommandTask(pduSender);
        try {
            executeSendCommand(task, getTransactionTimer());
        } catch (PDUException e) {
            // should never happen, since it doesn't have any String parameter.
         } catch (NegativeResponseException e) {
            // the command_status of the response should be always 0
        }
    }

    public void enquireLinkResp(int sequenceNumber) throws IOException {
        pduSender.sendEnquireLinkResp(connection().getOutputStream(), sequenceNumber);
    }

    public void genericNack(int commandStatus, int sequenceNumber) throws IOException {
        pduSender.sendGenericNack(connection().getOutputStream(), commandStatus, sequenceNumber);
    }

}
