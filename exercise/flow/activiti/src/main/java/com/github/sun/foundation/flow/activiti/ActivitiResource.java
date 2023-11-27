package com.github.sun.foundation.flow.activiti;

import com.github.sun.foundation.rest.AbstractResource;
import org.activiti.bpmn.model.BpmnModel;
import org.activiti.engine.*;
import org.activiti.engine.history.HistoricDetail;
import org.activiti.engine.runtime.Execution;
import org.activiti.engine.runtime.ProcessInstance;
import org.activiti.engine.task.Task;
import org.activiti.image.ProcessDiagramGenerator;
import org.springframework.beans.factory.annotation.Autowired;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Path("/v1/flow")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class ActivitiResource extends AbstractResource {
  @Autowired
  private RuntimeService runtimeService;
  @Autowired
  private TaskService taskService;
  @Autowired
  private RepositoryService repositoryService;
  @Autowired
  private ProcessEngine processEngine;
  @Autowired
  private HistoryService historyService;

  /**
   * 启动流程
   */
  @GET
  @Path("/start")
  public SingleResponse<String> startLeaveProcess(@QueryParam("staffId") String staffId) {
    Map<String, Object> map = new HashMap<>();
    map.put("taskUser", staffId);
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("Leave", map);
    StringBuilder sb = new StringBuilder();
    sb.append("创建请假流程 processId：").append(processInstance.getId());
    List<Task> tasks = taskService.createTaskQuery().taskAssignee(staffId).orderByTaskCreateTime().desc().list();
    for (Task task : tasks) {
      sb.append("; taskId=").append(task.getId()).append(" name=").append(task.getName());
    }
    return responseOf(sb.toString());
  }

  /**
   * 批准
   */
  @GET
  @Path("/approve")
  public SingleResponse<String> applyTask(@QueryParam("taskId") String taskId, @QueryParam("staffId") String staffId) {
    Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
    if (task == null) {
      throw new RuntimeException("流程不存在");
    }
    HashMap<String, Object> map = new HashMap<>();
    map.put("checkResult", "通过");
    map.put("taskUser" + staffId, staffId);
    map.put("days", 14);
    map.put("pass", true);
    taskService.complete(taskId, map);
    return responseOf("申请审核通过~");
  }

  /**
   * 驳回
   */
  @GET
  @Path("/reject")
  public String rejectTask(@QueryParam("taskId") String taskId) {
    HashMap<String, Object> map = new HashMap<>();
    map.put("checkResult", "驳回");
    taskService.complete(taskId, map);
    return "申请审核驳回~";
  }

  /**
   * 历史
   */
  @GET
  @Path("/his")
  public ListResponse<HistoricDetail> history(@QueryParam("processId") String processId) {
    List<HistoricDetail> list = historyService.createHistoricDetailQuery().list();
    return responseOf(list);
  }


  /**
   * 生成流程图
   */
  @GET
  @Path("/pic")
  public void createProcessDiagramPic(@Context HttpServletResponse httpServletResponse, @QueryParam("processId") String processId) throws Exception {
    ProcessInstance pi = runtimeService.createProcessInstanceQuery().processInstanceId(processId).singleResult();
    if (pi == null) {
      return;
    }
    Task task = taskService.createTaskQuery().processInstanceId(pi.getId()).singleResult();

    String InstanceId = task.getProcessInstanceId();
    List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(InstanceId).list();

    List<String> activityIds = new ArrayList<>();
    List<String> flows = new ArrayList<>();
    for (Execution exe : executions) {
      List<String> ids = runtimeService.getActiveActivityIds(exe.getId());
      activityIds.addAll(ids);
    }

    BpmnModel bpmnModel = repositoryService.getBpmnModel(pi.getProcessDefinitionId());
    ProcessEngineConfiguration engconf = processEngine.getProcessEngineConfiguration();
    ProcessDiagramGenerator diagramGenerator = engconf.getProcessDiagramGenerator();
    byte[] buf = new byte[1024];
    int legth = 0;
    try (InputStream in = diagramGenerator.generateDiagram(bpmnModel, "png", activityIds, flows, engconf.getActivityFontName(), engconf.getLabelFontName(), engconf.getAnnotationFontName(), engconf.getClassLoader(), 1.0); OutputStream out = httpServletResponse.getOutputStream()) {
      while ((legth = in.read(buf)) != -1) {
        out.write(buf, 0, legth);
      }
    }
  }
}
