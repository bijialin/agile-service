package io.choerodon.agile.api.controller.v1;

import com.alibaba.fastjson.JSONObject;


import io.choerodon.agile.infra.dto.UserDTO;
import io.choerodon.agile.infra.utils.EncryptionUtils;
import io.choerodon.core.domain.Page;
import io.choerodon.core.iam.InitRoleCode;
import io.choerodon.core.iam.ResourceLevel;
import io.choerodon.mybatis.pagehelper.domain.Sort;
import io.choerodon.swagger.annotation.Permission;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.hzero.starter.keyencrypt.core.Encrypt;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import springfox.documentation.annotations.ApiIgnore;
import io.choerodon.agile.api.vo.*;
import io.choerodon.agile.api.validator.IssueValidator;
import io.choerodon.agile.app.service.IssueService;
import io.choerodon.agile.app.service.StateMachineClientService;
import io.choerodon.agile.infra.dto.IssueConvertDTO;
import io.choerodon.agile.infra.utils.VerifyUpdateUtil;
import io.choerodon.core.exception.CommonException;
import io.choerodon.mybatis.pagehelper.annotation.SortDefault;
import io.choerodon.mybatis.pagehelper.domain.PageRequest;
import io.choerodon.swagger.annotation.CustomPageRequest;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Optional;

/**
 * 敏捷开发Issue
 *
 * @author dinghuang123@gmail.com
 * @since 2018-05-14 20:30:48
 */
@RestController
@RequestMapping(value = "/v1/projects/{project_id}/issues")
public class IssueController {

    private IssueService issueService;

    @Autowired
    private VerifyUpdateUtil verifyUpdateUtil;
    @Autowired
    private IssueValidator issueValidator;
    @Autowired
    private StateMachineClientService stateMachineClientService;

    public IssueController(IssueService issueService) {
        this.issueService = issueService;
    }

    @Permission(level = ResourceLevel.ORGANIZATION)
    @ApiOperation("创建issue")
    @PostMapping
    public ResponseEntity<IssueVO> createIssue(@ApiParam(value = "项目id", required = true)
                                                @PathVariable(name = "project_id") Long projectId,
                                               @ApiParam(value = "应用类型", required = true)
                                                @RequestParam(value = "applyType") String applyType,
                                               @ApiParam(value = "创建issue对象", required = true)
                                                @RequestBody IssueCreateVO issueCreateVO) {
        issueValidator.verifyCreateData(issueCreateVO, projectId, applyType);
        return Optional.ofNullable(stateMachineClientService.createIssue(issueCreateVO, applyType))
                .map(result -> new ResponseEntity<>(result, HttpStatus.CREATED))
                .orElseThrow(() -> new CommonException("error.Issue.createIssue"));
    }

    @Permission(level = ResourceLevel.ORGANIZATION)
    @ApiOperation("史诗名称重复校验")
    @GetMapping(value = "/check_epic_name")
    public ResponseEntity<Boolean> checkEpicName(@ApiParam(value = "项目id", required = true)
                                                 @PathVariable(name = "project_id") Long projectId,
                                                 @ApiParam(value = "史诗名称", required = true)
                                                 @RequestParam String epicName,
                                                 @RequestParam(required = false) @Encrypt Long epicId) {
        return Optional.ofNullable(issueService.checkEpicName(projectId, epicName, epicId))
                .map(result -> new ResponseEntity<>(result, HttpStatus.OK))
                .orElseThrow(() -> new CommonException("error.checkEpicName.get"));
    }

    @Permission(level = ResourceLevel.ORGANIZATION)
    @ApiOperation("创建issue子任务")
    @PostMapping(value = "/sub_issue")
    public ResponseEntity<IssueSubVO> createSubIssue(@ApiParam(value = "项目id", required = true)
                                                      @PathVariable(name = "project_id") Long projectId,
                                                     @ApiParam(value = "创建issue子任务对象", required = true)
                                                      @RequestBody IssueSubCreateVO issueSubCreateVO) {
        issueValidator.verifySubCreateData(issueSubCreateVO, projectId);
        return Optional.ofNullable(stateMachineClientService.createSubIssue(issueSubCreateVO))
                .map(result -> new ResponseEntity<>(result, HttpStatus.CREATED))
                .orElseThrow(() -> new CommonException("error.Issue.createSubIssue"));
    }

    @Permission(level = ResourceLevel.ORGANIZATION)
    @ApiOperation("更新issue")
    @PutMapping
    public ResponseEntity<IssueVO> updateIssue(@ApiParam(value = "项目id", required = true)
                                                @PathVariable(name = "project_id") Long projectId,
                                               @ApiParam(value = "更新issue对象", required = true)
                                                @RequestBody @Encrypt JSONObject issueUpdate) {
        issueValidator.verifyUpdateData(issueUpdate, projectId);
        IssueUpdateVO issueUpdateVO = new IssueUpdateVO();
        List<String> fieldList = verifyUpdateUtil.verifyUpdateData(issueUpdate,issueUpdateVO);
        return Optional.ofNullable(issueService.updateIssue(projectId, issueUpdateVO, fieldList))
                .map(result -> new ResponseEntity<>(result, HttpStatus.CREATED))
                .orElseThrow(() -> new CommonException("error.Issue.updateIssue"));
    }

    @Permission(level = ResourceLevel.ORGANIZATION)
    @ApiOperation("更新issue的状态")
    @PutMapping("/update_status")
    public ResponseEntity<IssueVO> updateIssueStatus(@ApiParam(value = "项目id", required = true)
                                                      @PathVariable(name = "project_id") Long projectId,
                                                     @ApiParam(value = "转换id", required = true)
                                                      @RequestParam Long transformId,
                                                     @ApiParam(value = "问题id", required = true)
                                                      @RequestParam @Encrypt Long issueId,
                                                     @ApiParam(value = "版本号", required = true)
                                                      @RequestParam Long objectVersionNumber,
                                                     @ApiParam(value = "应用类型", required = true)
                                                      @RequestParam String applyType) {
        return Optional.ofNullable(issueService.updateIssueStatus(projectId, issueId, transformId, objectVersionNumber, applyType))
                .map(result -> new ResponseEntity<>(result, HttpStatus.CREATED))
                .orElseThrow(() -> new CommonException("error.Issue.updateIssueStatus"));
    }

    @Permission(level = ResourceLevel.ORGANIZATION)
    @ApiOperation("查询单个issue")
    @GetMapping(value = "/{issueId}")
    public ResponseEntity<IssueVO> queryIssue(@ApiParam(value = "项目id", required = true)
                                               @PathVariable(name = "project_id") Long projectId,
                                              @ApiParam(value = "issueId", required = true)
                                               @PathVariable @Encrypt Long issueId,
                                              @ApiParam(value = "组织id", required = true)
                                               @RequestParam(required = false) Long organizationId) {
        return Optional.ofNullable(issueService.queryIssue(projectId, issueId, organizationId))
                .map(result -> new ResponseEntity<>(result, HttpStatus.OK))
                .orElseThrow(() -> new CommonException("error.Issue.queryIssue"));
    }

    @Permission(level = ResourceLevel.ORGANIZATION)
    @ApiOperation("查询单个子任务issue")
    @GetMapping(value = "/sub_issue/{issueId}")
    public ResponseEntity<IssueSubVO> queryIssueSub(@ApiParam(value = "项目id", required = true)
                                                     @PathVariable(name = "project_id") Long projectId,
                                                    @ApiParam(value = "组织id", required = true)
                                                     @RequestParam Long organizationId,
                                                    @ApiParam(value = "issueId", required = true)
                                                     @PathVariable @Encrypt Long issueId) {
        return Optional.ofNullable(issueService.queryIssueSub(projectId, organizationId, issueId))
                .map(result -> new ResponseEntity<>(result, HttpStatus.OK))
                .orElseThrow(() -> new CommonException("error.Issue.queryIssueSub"));
    }


    @Permission(level = ResourceLevel.ORGANIZATION)
    @ApiOperation("分页查询问题列表，包含子任务")
    @PostMapping(value = "/include_sub")
    public ResponseEntity<Page<IssueListFieldKVVO>> listIssueWithSub(@ApiIgnore
                                                               @ApiParam(value = "分页信息", required = true)
                                                               @SortDefault(value = "issueId", direction = Sort.Direction.DESC)
                                                                       PageRequest pageRequest,
                                                                         @ApiParam(value = "项目id", required = true)
                                                               @PathVariable(name = "project_id") Long projectId,
                                                                         @ApiParam(value = "查询参数", required = true)
                                                               @RequestBody(required = false) SearchVO searchVO,
                                                                         @ApiParam(value = "查询参数", required = true)
                                                               @RequestParam(required = false) Long organizationId) {
        EncryptionUtils.decryptSearchVO(searchVO);
        return Optional.ofNullable(issueService.listIssueWithSub(projectId, searchVO, pageRequest, organizationId))
                .map(result -> new ResponseEntity<>(result, HttpStatus.OK))
                .orElseThrow(() -> new CommonException("error.Issue.listIssueWithSub"));
    }

    @Permission(level = ResourceLevel.ORGANIZATION)
    @ApiOperation("分页搜索查询issue列表(包含子任务)")
    @CustomPageRequest
    @GetMapping(value = "/summary")
    public ResponseEntity<Page<IssueNumVO>> queryIssueByOption(@ApiIgnore
                                                                @ApiParam(value = "分页信息", required = true)
                                                                @SortDefault(value = "issueId", direction = Sort.Direction.DESC)
                                                                        PageRequest pageRequest,
                                                               @ApiParam(value = "项目id", required = true)
                                                                @PathVariable(name = "project_id") Long projectId,
                                                               @ApiParam(value = "issueId")
                                                                @RequestParam(required = false) @Encrypt Long issueId,
                                                               @ApiParam(value = "issueNum")
                                                                @RequestParam(required = false) String issueNum,
                                                               @ApiParam(value = "only active sprint", required = true)
                                                                @RequestParam Boolean onlyActiveSprint,
                                                               @ApiParam(value = "是否包含自身", required = true)
                                                                @RequestParam() Boolean self,
                                                               @ApiParam(value = "搜索内容", required = false)
                                                                @RequestParam(required = false) String content) {
        return Optional.ofNullable(issueService.queryIssueByOption(projectId, issueId, issueNum, onlyActiveSprint, self, content, pageRequest))
                .map(result -> new ResponseEntity<>(result, HttpStatus.OK))
                .orElseThrow(() -> new CommonException("error.Issue.queryIssueByOption"));
    }

    @Permission(level = ResourceLevel.ORGANIZATION)
    @ApiOperation("分页搜索查询issue列表")
    @CustomPageRequest
    @GetMapping(value = "/agile/summary")
    public ResponseEntity<Page<IssueNumVO>> queryIssueByOptionForAgile(@ApiIgnore
                                                                        @ApiParam(value = "分页信息", required = true)
                                                                        @SortDefault(value = "issueId", direction = Sort.Direction.DESC)
                                                                                PageRequest pageRequest,
                                                                           @ApiParam(value = "项目id", required = true)
                                                                        @PathVariable(name = "project_id") Long projectId,
                                                                           @ApiParam(value = "issueId")
                                                                        @RequestParam(required = false)  @Encrypt Long issueId,
                                                                           @ApiParam(value = "issueNum")
                                                                        @RequestParam(required = false) String issueNum,
                                                                           @ApiParam(value = "是否包含自身", required = true)
                                                                        @RequestParam() Boolean self,
                                                                           @ApiParam(value = "搜索内容")
                                                                        @RequestParam(required = false) String content) {
        return Optional.ofNullable(issueService.queryIssueByOptionForAgile(projectId, issueId, issueNum, self, content, pageRequest))
                .map(result -> new ResponseEntity<>(result, HttpStatus.OK))
                .orElseThrow(() -> new CommonException("error.Issue.queryIssueByOptionForAgile"));
    }


    @Permission(level = ResourceLevel.ORGANIZATION)
    @ApiOperation("查询epic")
    @GetMapping(value = "/epics")
    public ResponseEntity<List<EpicDataVO>> listEpic(@ApiParam(value = "项目id", required = true)
                                                      @PathVariable(name = "project_id") Long projectId) {
        return Optional.ofNullable(issueService.listEpic(projectId))
                .map(result -> new ResponseEntity<>(result, HttpStatus.OK))
                .orElseThrow(() -> new CommonException("error.Epic.listEpic"));
    }

    @Permission(level = ResourceLevel.ORGANIZATION)
    @ApiOperation("通过issueId删除")
    @DeleteMapping(value = "/{issueId}")
    public ResponseEntity deleteIssue(@ApiParam(value = "项目id", required = true)
                                      @PathVariable(name = "project_id") Long projectId,
                                      @ApiParam(value = "issueId", required = true)
                                      @PathVariable @Encrypt Long issueId) {
        issueService.deleteIssue(projectId, issueId);
        return new ResponseEntity(HttpStatus.NO_CONTENT);
    }

    @Permission(level = ResourceLevel.ORGANIZATION)
    @ApiOperation("删除自己创建的issue")
    @DeleteMapping(value = "/delete_self_issue/{issueId}")
    public ResponseEntity deleteSelfIssue(@ApiParam(value = "项目id", required = true)
                                      @PathVariable(name = "project_id") Long projectId,
                                      @ApiParam(value = "issueId", required = true)
                                      @PathVariable @Encrypt Long issueId) {
        issueService.deleteSelfIssue(projectId, issueId);
        return new ResponseEntity(HttpStatus.NO_CONTENT);
    }

    @Permission(level = ResourceLevel.ORGANIZATION)
    @ApiOperation("批量删除Issue,给测试")
    @DeleteMapping(value = "/to_version_test")
    public ResponseEntity batchDeleteIssues(@ApiParam(value = "项目id", required = true)
                                            @PathVariable(name = "project_id") Long projectId,
                                            @ApiParam(value = "issue id", required = true)
                                            @RequestBody @Encrypt  List<Long> issueIds) {
        issueService.batchDeleteIssues(projectId, issueIds);
        return new ResponseEntity(HttpStatus.NO_CONTENT);
    }

    @Permission(level = ResourceLevel.ORGANIZATION)
    @ApiOperation("issue批量加入版本")
    @PostMapping(value = "/to_version/{versionId}")
    public ResponseEntity<List<IssueSearchVO>> batchIssueToVersion(@ApiParam(value = "项目id", required = true)
                                                                    @PathVariable(name = "project_id") Long projectId,
                                                                   @ApiParam(value = "versionId", required = true)
                                                                    @PathVariable @Encrypt Long versionId,
                                                                   @ApiParam(value = "issue id", required = true)
                                                                    @RequestBody @Encrypt List<Long> issueIds) {
        return Optional.ofNullable(issueService.batchIssueToVersion(projectId, versionId, issueIds))
                .map(result -> new ResponseEntity<>(result, HttpStatus.CREATED))
                .orElseThrow(() -> new CommonException("error.issue.batchToVersion"));
    }

    @Permission(level = ResourceLevel.ORGANIZATION)
    @ApiOperation("批量替换issue版本,给测试")
    @PostMapping(value = "/to_version_test/{versionId}")
    public ResponseEntity batchIssueToVersionTest(@ApiParam(value = "项目id", required = true)
                                                  @PathVariable(name = "project_id") Long projectId,
                                                  @ApiParam(value = "versionId", required = true)
                                                  @PathVariable @Encrypt(ignoreValue = {"0"}) Long versionId,
                                                  @ApiParam(value = "issue id", required = true)
                                                  @RequestBody @Encrypt List<Long> issueIds) {
        issueService.batchIssueToVersionTest(projectId, versionId, issueIds);
        return new ResponseEntity(HttpStatus.NO_CONTENT);
    }

    @Permission(level = ResourceLevel.ORGANIZATION)
    @ApiOperation("issue批量加入epic")
    @PostMapping(value = "/to_epic/{epicId}")
    public ResponseEntity<List<IssueSearchVO>> batchIssueToEpic(@ApiParam(value = "项目id", required = true)
                                                                 @PathVariable(name = "project_id") Long projectId,
                                                                @ApiParam(value = "epicId", required = true)
                                                                 @PathVariable @Encrypt(ignoreValue = {"0"})  Long epicId,
                                                                @ApiParam(value = "issue id", required = true)
                                                                 @RequestBody @Encrypt List<Long> issueIds) {
        return Optional.ofNullable(issueService.batchIssueToEpic(projectId, epicId, issueIds))
                .map(result -> new ResponseEntity<>(result, HttpStatus.CREATED))
                .orElseThrow(() -> new CommonException("error.issue.batchToEpic"));
    }


    @Permission(level = ResourceLevel.ORGANIZATION)
    @ApiOperation("issue批量加入冲刺")
    @PostMapping(value = "/to_sprint/{sprintId}")
    public ResponseEntity<List<IssueSearchVO>> batchIssueToSprint(@ApiParam(value = "项目id", required = true)
                                                                   @PathVariable(name = "project_id") Long projectId,
                                                                  @ApiParam(value = "sprintId", required = true)
                                                                   @PathVariable @Encrypt(ignoreValue = {"0"}) Long sprintId,
                                                                  @ApiParam(value = "移卡信息", required = true)
                                                                   @RequestBody MoveIssueVO moveIssueVO) {
        return Optional.ofNullable(issueService.batchIssueToSprint(projectId, sprintId, moveIssueVO))
                .map(result -> new ResponseEntity<>(result, HttpStatus.CREATED))
                .orElseThrow(() -> new CommonException("error.issue.batchToSprint"));
    }

    @Permission(level = ResourceLevel.ORGANIZATION)
    @ApiOperation("查询当前项目下的epic，提供给列表下拉")
    @GetMapping(value = "/epics/select_data")
    public ResponseEntity<List<IssueEpicVO>> listEpicSelectData(@ApiParam(value = "项目id", required = true)
                                                                 @PathVariable(name = "project_id") Long projectId) {
        return Optional.ofNullable(issueService.listEpicSelectData(projectId))
                .map(result -> new ResponseEntity<>(result, HttpStatus.OK))
                .orElseThrow(() -> new CommonException("error.Issue.queryIssueEpicList"));
    }

    @Permission(level = ResourceLevel.ORGANIZATION)
    @ApiOperation("更改issue类型")
    @PostMapping("/update_type")
    public ResponseEntity<IssueVO> updateIssueTypeCode(@ApiParam(value = "项目id", required = true)
                                                        @PathVariable(name = "project_id") Long projectId,
                                                       @ApiParam(value = "组织id", required = true)
                                                        @RequestParam Long organizationId,
                                                       @ApiParam(value = "修改类型信息", required = true)
                                                        @RequestBody IssueUpdateTypeVO issueUpdateTypeVO) {
        IssueConvertDTO issueConvertDTO = issueValidator.verifyUpdateTypeData(projectId, issueUpdateTypeVO);
        return Optional.ofNullable(issueService.updateIssueTypeCode(issueConvertDTO, issueUpdateTypeVO, organizationId))
                .map(result -> new ResponseEntity<>(result, HttpStatus.CREATED))
                .orElseThrow(() -> new CommonException("error.issue.updateIssueTypeCode"));
    }

    @Permission(level = ResourceLevel.ORGANIZATION)
    @ApiOperation("任务转换为子任务")
    @PostMapping("/transformed_sub_task")
    public ResponseEntity<IssueSubVO> transformedSubTask(@ApiParam(value = "项目id", required = true)
                                                          @PathVariable(name = "project_id") Long projectId,
                                                         @ApiParam(value = "组织id", required = true)
                                                          @RequestParam Long organizationId,
                                                         @ApiParam(value = "转换子任务信息", required = true)
                                                          @RequestBody IssueTransformSubTask issueTransformSubTask) {
        issueValidator.verifyTransformedSubTask(issueTransformSubTask);
        return Optional.ofNullable(issueService.transformedSubTask(projectId, organizationId, issueTransformSubTask))
                .map(result -> new ResponseEntity<>(result, HttpStatus.CREATED))
                .orElseThrow(() -> new CommonException("error.issue.transformedSubTask"));
    }

    @Permission(level = ResourceLevel.ORGANIZATION)
    @ApiOperation("子任务转换为任务")
    @PostMapping("/transformed_task")
    public ResponseEntity<IssueVO> transformedTask(@ApiParam(value = "项目id", required = true)
                                                    @PathVariable(name = "project_id") Long projectId,
                                                   @ApiParam(value = "组织id", required = true)
                                                    @RequestParam Long organizationId,
                                                   @ApiParam(value = "转换任务信息", required = true)
                                                    @RequestBody IssueTransformTask issueTransformTask) {
        IssueConvertDTO issueConvertDTO = issueValidator.verifyTransformedTask(projectId, issueTransformTask);
        return Optional.ofNullable(issueService.transformedTask(issueConvertDTO, issueTransformTask, organizationId))
                .map(result -> new ResponseEntity<>(result, HttpStatus.CREATED))
                .orElseThrow(() -> new CommonException("error.issue.transformedTask"));
    }

    @ResponseBody
    @Permission(level = ResourceLevel.ORGANIZATION)
    @ApiOperation("导出issue列表")
    @PostMapping(value = "/export")
    public void exportIssues(@ApiIgnore
                             @ApiParam(value = "分页信息", required = true)
                             @SortDefault(value = "issueId", direction = Sort.Direction.DESC)
                             PageRequest pageRequest,
                             @ApiParam(value = "项目id", required = true)
                             @PathVariable(name = "project_id") Long projectId,
                             @ApiParam(value = "组织id", required = true)
                             @RequestParam Long organizationId,
                             @ApiParam(value = "查询参数", required = true)
                             @RequestBody(required = false) SearchVO searchVO,
                             HttpServletRequest request,
                             HttpServletResponse response) {
        EncryptionUtils.decryptSearchVO(searchVO);
        issueService.exportIssues(projectId, searchVO, request, response, organizationId, pageRequest.getSort());
    }


    @Permission(level = ResourceLevel.ORGANIZATION)
    @ApiOperation("复制一个issue")
    @PostMapping("/{issueId}/clone_issue")
    public ResponseEntity<IssueVO> cloneIssueByIssueId(@ApiParam(value = "项目id", required = true)
                                                        @PathVariable(name = "project_id") Long projectId,
                                                       @ApiParam(value = "issueId", required = true)
                                                        @PathVariable(name = "issueId") @Encrypt Long issueId,
                                                       @ApiParam(value = "组织id", required = true)
                                                        @RequestParam Long organizationId,
                                                       @ApiParam(value = "应用类型", required = true)
                                                        @RequestParam(value = "applyType") String applyType,
                                                       @ApiParam(value = "复制条件", required = true)
                                                        @RequestBody CopyConditionVO copyConditionVO) {
        return Optional.ofNullable(issueService.cloneIssueByIssueId(projectId, issueId, copyConditionVO, organizationId, applyType))
                .map(result -> new ResponseEntity<>(result, HttpStatus.CREATED))
                .orElseThrow(() -> new CommonException("error.issue.cloneIssueByIssueId"));
    }

    @Permission(level = ResourceLevel.ORGANIZATION)
    @ApiOperation("根据issue ids查询issue相关信息")
    @PostMapping("/issue_infos")
    public ResponseEntity<List<IssueInfoVO>> listByIssueIds(@ApiParam(value = "项目id", required = true)
                                                             @PathVariable(name = "project_id") Long projectId,
                                                            @ApiParam(value = "issue ids", required = true)
                                                             @RequestBody @Encrypt List<Long> issueIds) {
        return Optional.ofNullable(issueService.listByIssueIds(projectId, issueIds))
                .map(result -> new ResponseEntity<>(result, HttpStatus.CREATED))
                .orElseThrow(() -> new CommonException("error.issueNums.get"));
    }

    @Permission(level = ResourceLevel.ORGANIZATION)
    @ApiOperation("分页过滤查询issue列表提供给测试模块用")
    @CustomPageRequest
    @PostMapping(value = "/test_component/no_sub")
    public ResponseEntity<Page<IssueListTestVO>> listIssueWithoutSubToTestComponent(@ApiIgnore
                                                                                     @ApiParam(value = "分页信息", required = true)
                                                                                     @SortDefault(value = "issueId", direction = Sort.Direction.DESC)
                                                                                     PageRequest pageRequest,
                                                                                     @ApiParam(value = "项目id", required = true)
                                                                                     @PathVariable(name = "project_id") Long projectId,
                                                                                     @ApiParam(value = "组织id", required = true)
                                                                                     @RequestParam Long organizationId,
                                                                                     @ApiParam(value = "查询参数", required = true)
                                                                                     @RequestBody(required = false) SearchVO searchVO) {
        EncryptionUtils.decryptSearchVO(searchVO);
        return Optional.ofNullable(issueService.listIssueWithoutSubToTestComponent(projectId, searchVO, pageRequest, organizationId))
                .map(result -> new ResponseEntity<>(result, HttpStatus.OK))
                .orElseThrow(() -> new CommonException("error.Issue.listIssueWithoutSubToTestComponent"));
    }


    @Permission(level = ResourceLevel.PROJECT, roles = {InitRoleCode.PROJECT_MEMBER, InitRoleCode.PROJECT_OWNER})
    @ApiOperation("分页过滤查询issue列表, 测试项目接口，过滤linked issue")
    @CustomPageRequest
    @PostMapping(value = "/test_component/filter_linked")
    public ResponseEntity<Page<IssueListTestWithSprintVersionVO>> listIssueWithLinkedIssues(@ApiIgnore
                                                                                             @ApiParam(value = "分页信息", required = true)
                                                                                             @SortDefault(value = "issueId", direction = Sort.Direction.DESC)
                                                                                                     PageRequest pageable,
                                                                                                @ApiParam(value = "项目id", required = true)
                                                                                             @PathVariable(name = "project_id") Long projectId,
                                                                                                @ApiParam(value = "组织id", required = true)
                                                                                             @RequestParam Long organizationId,
                                                                                                @ApiParam(value = "查询参数", required = true)
                                                                                             @RequestBody(required = false) SearchVO searchVO) {
        EncryptionUtils.decryptSearchVO(searchVO);
        return Optional.ofNullable(issueService.listIssueWithLinkedIssues(projectId, searchVO, pageable, organizationId))
                .map(result -> new ResponseEntity<>(result, HttpStatus.OK))
                .orElseThrow(() -> new CommonException("error.Issue.listIssueWithBlockedIssues"));
    }

    @Permission(level = ResourceLevel.ORGANIZATION)
    @ApiOperation("根据时间段查询问题类型的数量")
    @GetMapping(value = "/type/{typeCode}")
    public ResponseEntity<List<IssueCreationNumVO>> queryIssueNumByTimeSlot(@ApiParam(value = "项目id", required = true)
                                                                             @PathVariable(name = "project_id") Long projectId,
                                                                            @ApiParam(value = "type code", required = true)
                                                                             @PathVariable String typeCode,
                                                                            @ApiParam(value = "时间段", required = true)
                                                                             @RequestParam Integer timeSlot) {
        return Optional.ofNullable(issueService.queryIssueNumByTimeSlot(projectId, typeCode, timeSlot))
                .map(result -> new ResponseEntity<>(result, HttpStatus.OK))
                .orElseThrow(() -> new CommonException("error.timeSlotCount.get"));
    }

    @Permission(level = ResourceLevel.ORGANIZATION)
    @ApiOperation(value = "拖动epic位置")
    @PutMapping(value = "/epic_drag")
    public ResponseEntity<EpicDataVO> dragEpic(@ApiParam(value = "项目id", required = true)
                                                @PathVariable(name = "project_id") Long projectId,
                                               @ApiParam(value = "排序对象", required = true)
                                                @RequestBody EpicSequenceVO epicSequenceVO) {
        return Optional.ofNullable(issueService.dragEpic(projectId, epicSequenceVO))
                .map(result -> new ResponseEntity<>(result, HttpStatus.CREATED))
                .orElseThrow(() -> new CommonException("error.issueController.dragEpic"));
    }

//    @Permission(level = ResourceLevel.ORGANIZATION)
//    @ApiOperation("统计issue相关信息（测试模块用）")
//    @PostMapping(value = "/test_component/statistic")
//    public ResponseEntity<List<PieChartVO>> issueStatistic(@ApiParam(value = "项目id", required = true)
//                                                            @PathVariable(name = "project_id") Long projectId,
//                                                           @ApiParam(value = "查询类型(version、component、label)", required = true)
//                                                            @RequestParam String type,
//                                                           @ApiParam(value = "需要排除的issue类型列表")
//                                                            @RequestBody List<String> issueTypes) {
//        return Optional.ofNullable(issueService.issueStatistic(projectId, type, issueTypes))
//                .map(result -> new ResponseEntity<>(result, HttpStatus.OK))
//                .orElseThrow(() -> new CommonException("error.Issue.issueStatistic"));
//    }

//    @Permission(level = ResourceLevel.PROJECT, roles = {InitRoleCode.PROJECT_MEMBER, InitRoleCode.PROJECT_OWNER})
//    @ApiOperation("分页过滤查询issue列表(不包含子任务，包含详情),测试模块用")
//    @CustomPageRequest
//    @PostMapping(value = "/test_component/no_sub_detail")
//    public ResponseEntity<PageInfo<IssueComponentDetailDTO>> listIssueWithoutSubDetail(@ApiIgnore
//                                                                                   @ApiParam(value = "分页信息", required = true)
//                                                                                   @SortDefault(value = "issueId", direction = Sort.Direction.DESC)
//                                                                                           Pageable pageable,
//                                                                                       @ApiParam(value = "项目id", required = true)
//                                                                                   @PathVariable(name = "project_id") Long projectId,
//                                                                                       @ApiParam(value = "查询参数", required = true)
//                                                                                   @RequestBody(required = false) SearchVO searchVO) {
//        return Optional.ofNullable(issueService.listIssueWithoutSubDetail(projectId, searchVO, pageable))
//                .map(result -> new ResponseEntity<>(result, HttpStatus.OK))
//                .orElseThrow(() -> new CommonException("error.Issue.listIssueWithoutSubDetail"));
//    }


    @Permission(level = ResourceLevel.ORGANIZATION)
    @ApiOperation("更改父任务")
    @PostMapping(value = "/update_parent")
    public ResponseEntity<IssueVO> updateIssueParentId(@ApiParam(value = "项目id", required = true)
                                                        @PathVariable(name = "project_id") Long projectId,
                                                       @ApiParam(value = "issue parent id update vo", required = true)
                                                        @RequestBody IssueUpdateParentIdVO issueUpdateParentIdVO) {
        return Optional.ofNullable(issueService.issueParentIdUpdate(projectId, issueUpdateParentIdVO))
                .map(result -> new ResponseEntity<>(result, HttpStatus.OK))
                .orElseThrow(() -> new CommonException("error.issueParentId.update"));
    }

    @Permission(level = ResourceLevel.ORGANIZATION)
    @ApiOperation("统计当前项目下未完成的任务数，包括故事、任务、缺陷")
    @GetMapping(value = "/count")
    public ResponseEntity<JSONObject> countUnResolveByProjectId(@ApiParam(value = "项目id", required = true)
                                                                @PathVariable(name = "project_id") Long projectId) {
        return Optional.ofNullable(issueService.countUnResolveByProjectId(projectId))
                .map(result -> new ResponseEntity<>(result, HttpStatus.OK))
                .orElseThrow(() -> new CommonException("error.countUnResolveIssue.get"));
    }

    @Permission(level = ResourceLevel.ORGANIZATION)
    @ApiOperation("根据条件过滤查询返回issueIds，测试项目接口")
    @PostMapping(value = "/issue_ids")
    public ResponseEntity<List<Long>> queryIssueIdsByOptions(@ApiParam(value = "项目id", required = true)
                                                             @PathVariable(name = "project_id") Long projectId,
                                                             @ApiParam(value = "查询参数", required = true)
                                                             @RequestBody SearchVO searchVO) {
        EncryptionUtils.decryptSearchVO(searchVO);
        return Optional.ofNullable(issueService.queryIssueIdsByOptions(projectId, searchVO))
                .map(result -> new ResponseEntity<>(result, HttpStatus.OK))
                .orElseThrow(() -> new CommonException("error.issueIds.get"));
    }

    @Permission(level = ResourceLevel.ORGANIZATION)
    @ApiOperation("查询未分配的问题，类型为story,task,bug")
    @GetMapping(value = "/undistributed")
    public ResponseEntity<Page<UndistributedIssueVO>> queryUnDistributedIssues(@ApiParam(value = "项目id", required = true)
                                                                                @PathVariable(name = "project_id") Long projectId,
                                                                                   @ApiParam(value = "分页信息", required = true)
                                                                                @ApiIgnore PageRequest pageRequest) {
        return Optional.ofNullable(issueService.queryUnDistributedIssues(projectId, pageRequest))
                .map(result -> new ResponseEntity<>(result, HttpStatus.OK))
                .orElseThrow(() -> new CommonException("error.UndistributedIssueList.get"));
    }

    @Permission(level = ResourceLevel.ORGANIZATION)
    @ApiOperation("查询经办人未完成的问题，类型为story,task,bug")
    @GetMapping(value = "/unfinished/{assignee_id}")
    public ResponseEntity<List<UnfinishedIssueVO>> queryUnfinishedIssues(@ApiParam(value = "项目id", required = true)
                                                                          @PathVariable(name = "project_id") Long projectId,
                                                                         @ApiParam(value = "经办人id", required = true)
                                                                          @PathVariable(name = "assignee_id") Long assigneeId) {
        return Optional.ofNullable(issueService.queryUnfinishedIssues(projectId, assigneeId))
                .map(result -> new ResponseEntity<>(result, HttpStatus.OK))
                .orElseThrow(() -> new CommonException("error.UnfinishedIssueList.get"));
    }

    @Permission(level = ResourceLevel.ORGANIZATION)
    @ApiOperation("查询用户故事地图泳道")
    @GetMapping(value = "/storymap/swim_lane")
    public ResponseEntity<String> querySwimLaneCode(@ApiParam(value = "项目id", required = true)
                                                    @PathVariable(name = "project_id") Long projectId) {
        return Optional.ofNullable(issueService.querySwimLaneCode(projectId))
                .map(result -> new ResponseEntity<>(result, HttpStatus.OK))
                .orElseThrow(() -> new CommonException("error.querySwimLaneCode.get"));
    }

//    @Permission(level = ResourceLevel.ORGANIZATION)
//    @ApiOperation("【测试专用】批量复制issue并生成版本信息")
//    @PostMapping("/batch_clone_issue/{versionId}")
//    public ResponseEntity<List<Long>> cloneIssuesByVersionId(@ApiParam(value = "项目id", required = true)
//                                                             @PathVariable(name = "project_id") Long projectId,
//                                                             @ApiParam(value = "versionId", required = true)
//                                                             @PathVariable Long versionId,
//                                                             @ApiParam(value = "复制的issueIds", required = true)
//                                                             @RequestBody List<Long> issueIds) {
//        issueValidator.checkIssueIdsAndVersionId(projectId, issueIds, versionId);
//        return Optional.ofNullable(issueService.cloneIssuesByVersionId(projectId, versionId, issueIds))
//                .map(result -> new ResponseEntity<>(result, HttpStatus.CREATED))
//                .orElseThrow(() -> new CommonException("error.issue.cloneIssuesByVersionId"));
//    }

//    @Permission(level = ResourceLevel.ORGANIZATION)
//    @ApiOperation("【测试专用】issue按照项目分组接口")
//    @GetMapping("/list_issues_by_project")
//    public ResponseEntity<List<IssueProjectVO>> queryIssueTestGroupByProject(@ApiParam(value = "项目id", required = true)
//                                                                              @PathVariable(name = "project_id") Long projectId) {
//        return Optional.ofNullable(issueService.queryIssueTestGroupByProject())
//                .map(result -> new ResponseEntity<>(result, HttpStatus.OK))
//                .orElseThrow(() -> new CommonException("error.issue.queryIssueTestGroupByProject"));
//    }

//
//    @Permission(level = ResourceLevel.ORGANIZATION)
//    @ApiOperation("【测试专用】根据issueNum查询issue")
//    @PostMapping(value = "/query_by_issue_num")
//    public ResponseEntity<IssueNumDTO> queryIssueByIssueNum(@ApiParam(value = "项目id", required = true)
//                                                            @PathVariable(name = "project_id") Long projectId,
//                                                            @ApiParam(value = "issue编号", required = true)
//                                                            @RequestBody String issueNum) {
//        return Optional.ofNullable(issueService.queryIssueByIssueNum(projectId, issueNum))
//                .map(result -> new ResponseEntity<>(result, HttpStatus.OK))
//                .orElseThrow(() -> new CommonException("error.issue.queryIssueByIssueNum"));
//    }

    @Permission(level = ResourceLevel.ORGANIZATION)
    @ApiOperation("【测试专用】根据issueIds查询issue")
    @PostMapping(value = "/query_issue_ids")
    public ResponseEntity<List<IssueLinkVO>> queryIssues(@ApiParam(value = "项目id", required = true)
                                                         @PathVariable(name = "project_id") Long projectId,
                                                         @ApiParam(value = "issue编号", required = true)
                                                         @RequestBody @Encrypt List<Long> issueIds) {
        return Optional.ofNullable(issueService.queryIssueByIssueIds(projectId, issueIds))
                .map(result -> new ResponseEntity<>(result, HttpStatus.OK))
                .orElseThrow(() -> new CommonException("error.issue.queryIssueByIssueIds"));
    }

    @Permission(level = ResourceLevel.ORGANIZATION)
    @ApiOperation("查询项目下的故事和任务(不包含子任务以及子bug)")
    @PostMapping(value = "/query_story_task")
    public ResponseEntity<Page<IssueListFieldKVVO>> queryStoryAndTask(@ApiParam(value = "项目id", required = true)
                                                         @PathVariable(name = "project_id") Long projectId,
                                                         @SortDefault PageRequest pageRequest,
                                                          @RequestBody(required = false) SearchVO searchVO) {
        return Optional.ofNullable(issueService.queryStoryAndTask(projectId, pageRequest, searchVO))
                .map(result -> new ResponseEntity<>(result, HttpStatus.OK))
                .orElseThrow(() -> new CommonException("error.issue.queryIssueByIssueIds"));
    }


    @CustomPageRequest
    @Permission(level = ResourceLevel.ORGANIZATION)
    @ApiOperation("查询项目所有经办人")
    @GetMapping(value = "/users")
    public ResponseEntity<Page<UserDTO>> pagingQueryUsers(@ApiIgnore
                                                              @ApiParam(value = "分页信息", required = true)
                                                              PageRequest pageRequest,
                                                              @ApiParam(value = "项目id", required = true)
                                                              @PathVariable(name = "project_id") Long projectId,
                                                              @RequestParam(value = "param", required = false) String param) {
        return ResponseEntity.ok(issueService.pagingQueryUsers(pageRequest, projectId, param));
    }

    @CustomPageRequest
    @Permission(level = ResourceLevel.ORGANIZATION)
    @ApiOperation("查询项目所有报告人")
    @GetMapping(value = "/reporters")
    public ResponseEntity<Page<UserDTO>> pagingQueryReporters(@ApiIgnore
                                                                  @ApiParam(value = "分页信息", required = true)
                                                                  PageRequest pageRequest,
                                                                  @ApiParam(value = "项目id", required = true)
                                                                  @PathVariable(name = "project_id") Long projectId,
                                                                  @RequestParam(value = "param", required = false) String param) {
        return ResponseEntity.ok(issueService.pagingQueryReporters(pageRequest, projectId, param));
    }
}