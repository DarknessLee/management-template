package com.mgmt.module.file;

import com.mgmt.common.PageResult;
import com.mgmt.common.Result;
import com.mgmt.module.file.dto.FileVO;
import com.mgmt.security.OperationLog;
import com.mgmt.security.RequirePermission;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.InvalidMediaTypeException;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/files")
@RequiredArgsConstructor
public class FileController {

    private final FileService fileService;

    @GetMapping
    @RequirePermission("file:view")
    public Result<PageResult<FileVO>> page(@RequestParam(defaultValue = "1") long page,
                                           @RequestParam(defaultValue = "10") long size,
                                           @RequestParam(required = false) String keyword) {
        return Result.ok(fileService.page(page, size, keyword));
    }

    @PostMapping
    @RequirePermission("file:edit")
    @OperationLog(module = "文件管理", action = "上传文件")
    public Result<FileVO> upload(@RequestParam("file") MultipartFile file) {
        return Result.ok(fileService.store(file));
    }

    /**
     * 通过接口下载而不是直接暴露静态地址：文件名可带中文，且 Content-Disposition 强制附件下载。
     */
    @GetMapping("/{id}/download")
    @RequirePermission("file:view")
    public ResponseEntity<byte[]> download(@PathVariable Long id) {
        FileService.FileDownload download = fileService.download(id);
        ContentDisposition disposition = ContentDisposition.attachment()
                .filename(download.filename(), StandardCharsets.UTF_8)
                .build();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .contentType(mediaTypeOf(download.contentType()))
                .body(download.content());
    }

    @DeleteMapping("/{id}")
    @RequirePermission("file:edit")
    @OperationLog(module = "文件管理", action = "删除文件")
    public Result<Void> delete(@PathVariable Long id) {
        fileService.delete(id);
        return Result.ok();
    }

    /**
     * MIME 来自客户端，可能不是合法格式；解析失败时退回二进制流，避免下载接口 500。
     */
    private MediaType mediaTypeOf(String contentType) {
        try {
            return MediaType.parseMediaType(contentType);
        } catch (InvalidMediaTypeException e) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
    }
}
