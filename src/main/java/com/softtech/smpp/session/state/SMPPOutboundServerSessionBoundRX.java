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
package com.softtech.smpp.session.state;

import java.io.IOException;

import com.softtech.smpp.PDUStringException;
import com.softtech.smpp.bean.Command;
import com.softtech.smpp.bean.DeliverSm;
import com.softtech.smpp.extra.ProcessRequestException;
import com.softtech.smpp.extra.SessionState;
import com.softtech.smpp.session.OutboundServerResponseHandler;
import com.softtech.smpp.util.DefaultDecomposer;
import com.softtech.smpp.util.PDUDecomposer;

/**
 * This class is bound_rx state implementation of {@link SMPPOutboundServerSessionState}.
 * Response to receiver related transaction.
 * 
 * @author uudashr
 * @version 1.0
 * @since 2.3
 * 
 */
class SMPPOutboundServerSessionBoundRX extends SMPPOutboundServerSessionBound
    implements SMPPOutboundServerSessionState {
     private static final PDUDecomposer pduDecomposer = new DefaultDecomposer();
    
    public SessionState getSessionState() {
        return SessionState.BOUND_RX;
    }
    
    public void processDeliverSm(Command pduHeader, byte[] pdu,
            OutboundServerResponseHandler responseHandler) throws IOException {
        processDeliverSm0(pduHeader, pdu, responseHandler);
    }

    static void processDeliverSm0(Command pduHeader, byte[] pdu,
            OutboundServerResponseHandler responseHandler) throws IOException {
        try {
            DeliverSm deliverSm = pduDecomposer.deliverSm(pdu);
            responseHandler.processDeliverSm(deliverSm);
            responseHandler.sendDeliverSmResp(0, pduHeader.getSequenceNumber(), deliverSm.getId());
        } catch (PDUStringException e) {
            responseHandler.sendGenerickNack(e.getErrorCode(), pduHeader
                .getSequenceNumber());
        } catch (ProcessRequestException e) {
            responseHandler.sendDeliverSmResp(e.getErrorCode(), pduHeader.getSequenceNumber(), null);
        }
    }
    
    
}
