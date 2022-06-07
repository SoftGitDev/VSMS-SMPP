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

import com.softtech.smpp.InvalidResponseException;
import com.softtech.smpp.PDUException;
import com.softtech.smpp.PDUSender;
import com.softtech.smpp.bean.DataCoding;
import com.softtech.smpp.bean.ESMClass;
import com.softtech.smpp.bean.MessageState;
import com.softtech.smpp.bean.NumberingPlanIndicator;
import com.softtech.smpp.bean.OptionalParameter;
import com.softtech.smpp.bean.RegisteredDelivery;
import com.softtech.smpp.bean.TypeOfNumber;
import com.softtech.smpp.bean.UnsuccessDelivery;
import com.softtech.smpp.extra.NegativeResponseException;
import com.softtech.smpp.extra.ResponseTimeoutException;
import com.softtech.smpp.session.connection.Connection;
import com.softtech.smpp.util.MessageId;

/**
 * @author SUTHAR
 * 
 */
public class DefaultSMPPServerOperation extends AbstractSMPPOperation implements
        SMPPServerOperation {

    public DefaultSMPPServerOperation(Connection connection, PDUSender pduSender) {
        super(connection, pduSender);
    }

    public void deliverSm(String serviceType, TypeOfNumber sourceAddrTon,
            NumberingPlanIndicator sourceAddrNpi, String sourceAddr,
            TypeOfNumber destAddrTon, NumberingPlanIndicator destAddrNpi,
            String destinationAddr, ESMClass esmClass, byte protocoId,
            byte priorityFlag, RegisteredDelivery registeredDelivery,
            DataCoding dataCoding, byte[] shortMessage,
            OptionalParameter... optionalParameters) throws PDUException,
            ResponseTimeoutException, InvalidResponseException,
            NegativeResponseException, IOException {

        DeliverSmCommandTask task = new DeliverSmCommandTask(pduSender(),
                serviceType, sourceAddrTon, sourceAddrNpi, sourceAddr,
                destAddrTon, destAddrNpi, destinationAddr, esmClass, protocoId,
                protocoId, registeredDelivery, dataCoding, shortMessage,
                optionalParameters);

        executeSendCommand(task, getTransactionTimer());
    }

    public void alertNotification(int sequenceNumber,
            TypeOfNumber sourceAddrTon, NumberingPlanIndicator sourceAddrNpi,
            String sourceAddr, TypeOfNumber esmeAddrTon,
            NumberingPlanIndicator esmeAddrNpi, String esmeAddr,
            OptionalParameter... optionalParameters) throws PDUException,
            IOException {
        pduSender().sendAlertNotification(connection().getOutputStream(),
                sequenceNumber, sourceAddrTon.value(), sourceAddrNpi.value(),
                sourceAddr, esmeAddrTon.value(), esmeAddrNpi.value(), esmeAddr,
                optionalParameters);
    }

    public void querySmResp(String messageId, String finalDate,
            MessageState messageState, byte errorCode, int sequenceNumber)
            throws PDUException, IOException {
        pduSender().sendQuerySmResp(connection().getOutputStream(),
                sequenceNumber, messageId, finalDate, messageState, errorCode);
    }

    public void replaceSmResp(int sequenceNumber) throws IOException {
        pduSender().sendReplaceSmResp(connection().getOutputStream(),
                sequenceNumber);
    }

    public void submitMultiResp(int sequenceNumber, String messageId,
            UnsuccessDelivery... unsuccessDeliveries) throws PDUException,
            IOException {
        pduSender().sendSubmitMultiResp(connection().getOutputStream(),
                sequenceNumber, messageId, unsuccessDeliveries);
    }

    public void submitSmResp(MessageId messageId, int sequenceNumber)
            throws PDUException, IOException {
        pduSender().sendSubmitSmResp(connection().getOutputStream(),
                sequenceNumber, messageId.getValue());
    }
}
