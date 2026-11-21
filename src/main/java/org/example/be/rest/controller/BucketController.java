package org.example.be.rest.controller;

import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.util.List;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.example.be.rest.dto.req.bucket.CreateBucketReq;
import org.example.be.rest.dto.req.bucket.EditBucketReq;
import org.example.be.rest.dto.res.bucket.BucketListRes;
import org.example.be.rest.dto.res.bucket.BucketMatchingRes;
import org.example.be.rest.dto.res.bucket.BucketPreviewRes;
import org.example.be.rest.dto.res.bucket.BucketRes;
import org.example.be.core.util.ApiRes;
import org.example.be.rest.usecase.bucket.CreateBucketUseCase;
import org.example.be.rest.usecase.bucket.DetailBucketUseCase;
import org.example.be.rest.usecase.bucket.MatchingBucketUseCase;
import org.example.be.rest.usecase.bucket.PreviewBucketUseCase;
import org.example.be.rest.usecase.bucket.SearchBucketUseCase;
import org.example.be.rest.usecase.bucket.UpdateBucketUseCase;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/buckets")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class BucketController {

    CreateBucketUseCase createBucketUseCase;
    SearchBucketUseCase searchBucketUseCase;
    UpdateBucketUseCase updateBucketUseCase;
    DetailBucketUseCase detailBucketUseCase;
    MatchingBucketUseCase matchingBucketUseCase;
    PreviewBucketUseCase previewBucketUseCase;

    @GetMapping
    public ApiRes<BucketListRes> search(@AuthenticationPrincipal Jwt jwt,
                                         @RequestParam(required = false) String month) {
        return ApiRes.success(HttpStatus.OK.value(), searchBucketUseCase.execute(jwt.getSubject(), month));
    }

    @PostMapping
    public ApiRes<BucketRes> create(@AuthenticationPrincipal Jwt jwt, @RequestBody @Valid CreateBucketReq req) {
        var result = createBucketUseCase.execute(jwt.getSubject(), req);
        return ApiRes.success(result.alreadyExisted() ? HttpStatus.OK.value() : HttpStatus.CREATED.value(), result.record());
    }

    @PatchMapping("/{bucketId}")
    public ApiRes<BucketRes> update(@AuthenticationPrincipal Jwt jwt, @PathVariable String bucketId,
                                     @RequestBody @Valid EditBucketReq req) {
        return ApiRes.success(HttpStatus.OK.value(), updateBucketUseCase.execute(jwt.getSubject(), bucketId, req));
    }

    @GetMapping("/matching")
    public ApiRes<BucketMatchingRes> matching(@AuthenticationPrincipal Jwt jwt,
                                               @RequestParam String categoryId) {
        return ApiRes.success(HttpStatus.OK.value(), matchingBucketUseCase.execute(jwt.getSubject(), categoryId));
    }

    @GetMapping("/preview")
    public ApiRes<BucketPreviewRes> preview(@AuthenticationPrincipal Jwt jwt,
                                             @RequestParam List<String> categoryIds,
                                             @RequestParam BigDecimal monthlyTarget,
                                             @RequestParam(required = false) String bucketId) {
        return ApiRes.success(HttpStatus.OK.value(),
                previewBucketUseCase.execute(jwt.getSubject(), categoryIds, monthlyTarget, bucketId));
    }

    @GetMapping("/{bucketId}")
    public ApiRes<BucketRes> detail(@AuthenticationPrincipal Jwt jwt, @PathVariable String bucketId,
                                     @RequestParam(required = false) String month) {
        return ApiRes.success(HttpStatus.OK.value(), detailBucketUseCase.execute(jwt.getSubject(), bucketId, month));
    }

}
