package cn.driftcloud.photo.controller

/**
 * @author DriftCloud
 * @Description
 * @Date 2026/1/21
 */
import arrow.core.flatMap
import cn.driftcloud.common.annotation.CurrentUserId
import cn.driftcloud.common.annotation.RateLimit
import cn.driftcloud.common.pojo.vo.CursorPageVO
import cn.driftcloud.common.response.R
import cn.driftcloud.common.util.FileValidator.validateImage
import cn.driftcloud.photo.entity.Photo
import cn.driftcloud.photo.mapper.PhotoMapper
import cn.driftcloud.photo.pojo.PhotoVO
import cn.driftcloud.photo.service.ImgProxyService
import cn.driftcloud.photo.service.PhotoService
import cn.driftcloud.photo.service.PhotoTimeStatService
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper
import com.baomidou.mybatisplus.extension.kotlin.KtQueryWrapper
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import java.sql.Timestamp
import java.time.Instant


@RestController
@RequestMapping("/photo")
class PhotoController(
    private val photoMapper: PhotoMapper,
    private val photoService: PhotoService,
    private val photoTimeStatService: PhotoTimeStatService,
    private val imgProxyService: ImgProxyService
) {

    /**
     * 上传图片
     */
    @PostMapping("/upload")
    fun upload(
        @CurrentUserId userId: Long,
        @RequestParam("photo") photo: MultipartFile
    ): R<out PhotoVO?> {
        // 效验文件
        return validateImage(photo)
            .flatMap {
                meta -> photoService.uploadPhoto(photo, meta, userId)
            }
            .fold(
                { err -> R.error(err) },
                { vo ->
                    // 更新照片时间统计
                    photoTimeStatService.addPhotoTimeStat(vo)
                    R.success(vo)
                }
            )
    }

    @PostMapping("/upload/with-created-at")
    fun uploadWithCreatedAt(
        @CurrentUserId userId: Long,
        @RequestParam("photo") photo: MultipartFile,
        @RequestParam("createdAt") createdAt: Instant
    ): R<out PhotoVO?> {
        // 效验用户, 只可以是userId为1(管理员)上传
        if (userId != 1L) {
            return R.error("只有管理员可以上传指定时间的照片")
        }
        // 效验文件
        return validateImage(photo)
            .flatMap {
                meta -> photoService.uploadPhoto(photo, meta, userId, createdAt)
            }
            .fold(
                { err -> R.error(err) },
                { vo ->
                    // 更新照片时间统计
                    photoTimeStatService.addPhotoTimeStat(vo)
                    R.success(vo)
                }
            )
    }

    /**
     * 获取图片游标分页
     */
    @GetMapping("/cursor")
    @RateLimit(key = "#userId", time = 10, limit = 10)
    fun getPhotos(
        @CurrentUserId userId: Long,
        @RequestParam(required = false) cursor: Instant?,
        @RequestParam(defaultValue = "100") size: Int,
        @RequestParam(defaultValue = "next") direction: String,
        @RequestParam(required = false) defaultCollectionId: String?
    ): R<out CursorPageVO<PhotoVO, Instant>?> {
        if (direction !in listOf("next", "prev")) {
            return R.error("分页方向错误")
        }
        return R.ok(
            photoService.getPhotoCursorPage(
                userId,
                cursor,
                size,
                direction,
                defaultCollectionId?.toLongOrNull()
            )
        )
    }

    /**
     * 校验MD5是否存在
     */
    @GetMapping("/md5-exist")
    fun md5Exist(@RequestParam md5: String): R<out Boolean?> {
        return R.ok(photoService.md5Exist(md5))
    }

    /**
     * 根据时间查询图片
     * @param time 时间戳，毫秒级
     * @param size 每页数量
     * @return 图片列表
     */
    @GetMapping("/by-time")
    fun getPhotoByTime(
        @RequestParam time: Long,
        @RequestParam size: Int
    ): R<out CursorPageVO<PhotoVO, Instant>?> {
        val queryWrapper = KtQueryWrapper(Photo::class.java)
            .orderByDesc(Photo::createdAt)
            .lt(Photo::createdAt, Instant.ofEpochMilli(time))
            .last("limit $size")

        val list = photoMapper.selectList(queryWrapper)
        val nextCursor = list.lastOrNull()?.createdAt
        return R.ok(
            CursorPageVO(
                records = list.map { imgProxyService.photoToVO(it) },
                nextCursor = nextCursor,
                hasMore = list.size == size
            )
        )
    }

    /**
     * 获取图片时间范围
     * @return 时间范围，包含最小时间和最大时间，格式为Map，键为"min"和"max"，值为Instant类型
     */
    @GetMapping("/time-range")
    fun getTimeRange(): R<Map<String, Instant?>> {
        val wrapper = QueryWrapper<Photo>()
            .select("MIN(created_at) as min", "MAX(created_at) as max")
        val map = photoMapper.selectMaps(wrapper).firstOrNull()
        val min = (map?.get("min") as? Timestamp)?.toInstant() ?: Instant.now()
        val max = (map?.get("max") as? Timestamp)?.toInstant() ?: Instant.now()
        return R.ok(mapOf(
            "min" to min,
            "max" to max
        ))
    }

}