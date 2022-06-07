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

import com.softtech.smpp.bean.Command;
import com.softtech.smpp.bean.DeliverSmResp;
import com.softtech.smpp.extra.PendingResponse;
import com.softtech.smpp.extra.SessionState;
import com.softtech.smpp.session.OutboundResponseHandler;
import com.softtech.smpp.util.DefaultDecomposer;
import com.softtech.smpp.util.PDUDecomposer;

/**
 * This class is bound_rx state implementation of {@link SMPPOutboundSessionState}.
 * Response to receiver related transaction.
 * 
 * @author uudashr
 * @version 1.0
 * @since 2.3
 * 
 */
class SMPPOutboundSessionBoundRX extends SMPPOutboundSessionBound implements SMPPOutboundSessionState {
     private static final PDUDecomposer pduDecomposer = new DefaultDecomposer();
    
    public SessionState getSessionState() {
        return SessionState.BOUND_RX;
    }

    public void processDeliverSmResp(Command pduHeader, byte[] pdu,
                                     OutboundResponseHandler responseHandler) throws IOException {
        processDeliverSmResp0(pduHeader, pdu, responseHandler);
    }

    static final void processDeliverSmResp0(Command pduHeader, byte[] pdu,
                                            OutboundResponseHandler responseHandler) throws IOException {
        PendingResponse<Command> pendingResp = responseHandler.removeSentItem(pduHeader.getSequenceNumber());
        if (pendingResp != null) {
            DeliverSmResp resp = pduDecomposer.deliverSmResp(pdu);
            pendingResp.done(resp);
        } else {
        }
    }
}
