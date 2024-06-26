/*
 *  Copyright 2020 Xiaomi
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package com.xiaomi.youpin.docean.exception;

import lombok.Getter;

/**
 * @author goodjava@qq.com
 * @date 2020/6/20
 */
public class DoceanException extends RuntimeException {

    @Getter
    private int code;

    public DoceanException(Throwable ex) {
        super(ex);
    }


    public DoceanException() {
    }

    public DoceanException(String m) {
        super(m);
    }

    public DoceanException(String message, int code) {
        super(message);
        this.code = code;
    }
}
