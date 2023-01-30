/*
 * Copyright (c) 2022-2022 Huawei Technologies Co.,Ltd.
 *
 * openGauss is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 *
 *           http://license.coscl.org.cn/MulanPSL2
 *
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND,
 * EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT,
 * MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.opengauss.portalcontroller;

/**
 * The type Thread exec plan.
 */
public class ThreadExecPlan extends Thread implements Runnable {

    private String workspaceId;

    /**
     * Gets workspace id.
     *
     * @return the workspace id
     */
    public String getWorkspaceId() {
        return workspaceId;
    }

    /**
     * Sets workspace id.
     *
     * @param workspaceId the workspace id
     */
    public void setWorkspaceId(String workspaceId) {
        this.workspaceId = workspaceId;
    }

    /**
     * Create a new thread exec plan.
     *
     * @param workspaceId the workspace id
     */
    public ThreadExecPlan (String workspaceId){
        this.workspaceId = workspaceId;
    }
    @Override
    public void run() {
        Plan.getInstance(this.workspaceId).execPlan(PortalControl.taskList);
    }
}
