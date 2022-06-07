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
package com.softtech.smpp.bean;

import com.softtech.smpp.SMPPConstant;

/**
 * @author SUTHAR
 * 
 */
public class EnquireLink extends Command {
    private static final long serialVersionUID = -2906795675909484142L;

    public EnquireLink(int sequenceNumber) {
        super();
        commandLength = 16;
        commandId = SMPPConstant.CID_ENQUIRE_LINK;
        commandStatus = 0;
        this.sequenceNumber = sequenceNumber;
    }

    public EnquireLink() {
        super();
    }

}
