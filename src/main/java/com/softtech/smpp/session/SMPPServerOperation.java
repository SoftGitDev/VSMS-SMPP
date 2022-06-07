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
import com.softtech.smpp.util.MessageId;

/**
 * @author SUTHAR
 * 
 */
public interface SMPPServerOperation extends SMPPOperation {

    void deliverSm(String serviceType, TypeOfNumber sourceAddrTon,
            NumberingPlanIndicator sourceAddrNpi, String sourceAddr,
            TypeOfNumber destAddrTon, NumberingPlanIndicator destAddrNpi,
            String destinationAddr, ESMClass esmClass, byte protocoId,
            byte priorityFlag, RegisteredDelivery registeredDelivery,
            DataCoding dataCoding, byte[] shortMessage,
            OptionalParameter... optionalParameters) throws PDUException,
            ResponseTimeoutException, InvalidResponseException,
            NegativeResponseException, IOException;

    void alertNotification(int sequenceNumber, TypeOfNumber sourceAddrTon,
            NumberingPlanIndicator sourceAddrNpi, String sourceAddr,
            TypeOfNumber esmeAddrTon, NumberingPlanIndicator esmeAddrNpi,
            String esmeAddr, OptionalParameter... optionalParameters)
            throws PDUException, IOException;

    void submitSmResp(MessageId messageId, int sequenceNumber)
            throws PDUException, IOException;

    void submitMultiResp(int sequenceNumber, String messageId,
            UnsuccessDelivery... unsuccessDeliveries) throws PDUException,
            IOException;

    void querySmResp(String messageId, String finalDate,
            MessageState messageState, byte errorCode, int sequenceNumber)
            throws PDUException, IOException;

    void replaceSmResp(int sequenceNumber) throws IOException;
}
