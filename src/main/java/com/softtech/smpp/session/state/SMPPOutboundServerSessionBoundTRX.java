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
import com.softtech.smpp.extra.SessionState;
import com.softtech.smpp.session.OutboundServerResponseHandler;

/**
 * This class is bound_trx state implementation of {@link SMPPOutboundServerSessionState}.
 * Response both to transmit and receive related transaction.
 * 
 * @author uudashr
 * @version 1.0
 * @since 2.3
 * 
 */
class SMPPOutboundServerSessionBoundTRX extends SMPPOutboundServerSessionBoundTX
    implements SMPPOutboundServerSessionState {
    
    @Override
    public SessionState getSessionState() {
        return SessionState.BOUND_TRX;
    }
    
    @Override
    public void processDeliverSm(Command pduHeader, byte[] pdu,
            OutboundServerResponseHandler responseHandler) throws IOException {
        SMPPOutboundServerSessionBoundRX.processDeliverSm0(pduHeader, pdu, responseHandler);
    }

}
