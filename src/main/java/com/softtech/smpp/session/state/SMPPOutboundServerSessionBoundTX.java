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

import com.softtech.smpp.SMPPConstant;
import com.softtech.smpp.bean.Command;
import com.softtech.smpp.extra.SessionState;
import com.softtech.smpp.session.OutboundServerResponseHandler;

/**
 * This class is bound_tx state implementation of {@link SMPPOutboundServerSessionState}. This
 * class give specific response to a transmit related transaction, otherwise
 * it always give negative response.
 * 
 * @author uudashr
 * @version 1.0
 * @since 2.3
 * 
 */
class SMPPOutboundServerSessionBoundTX extends SMPPOutboundServerSessionBound
    implements SMPPOutboundServerSessionState {
    
    public SessionState getSessionState() {
        return SessionState.BOUND_TX;
    }

    public void processDeliverSm(Command pduHeader, byte[] pdu,
                                 OutboundServerResponseHandler responseHandler) throws IOException {
        responseHandler.sendNegativeResponse(pduHeader.getCommandId(),
            SMPPConstant.STAT_ESME_RINVBNDSTS, pduHeader
                .getSequenceNumber());
    }
}
