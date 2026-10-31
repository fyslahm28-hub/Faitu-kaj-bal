package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.AdminRegistryEntry
import com.example.data.model.AppNotification
import com.example.data.model.AppReleaseInfo
import com.example.data.model.BookmarkItem
import com.example.data.model.ChatGroup
import com.example.data.model.DirectMessage
import com.example.data.model.FeedComment
import com.example.data.model.FeedPost
import com.example.data.model.KhedmotCategoryHelper
import com.example.data.model.KhedmotSubmission
import com.example.data.model.MadrasaProfile
import com.example.data.model.TelegramNotificationLog
import com.example.data.model.UserProfile
import com.example.data.repository.QawmiRepository
import com.example.data.repository.formatFirestoreErrorBangla
import com.example.data.repository.isRetryableFirestoreMessageError
import com.example.ui.util.AdminSecurityHelper
import com.example.ui.util.ProfilePhotoHelper
import com.google.firebase.Timestamp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

data class FailedMessageDraft(
    val partnerId: String,
    val body: String,
    val imageUrls: List<String>,
    val messageId: String = "",
    val receiverName: String = "",
    val subject: String = "খেদমত সংক্রান্ত যোগাযোগ",
    val groupId: String = "",
    val groupName: String = "",
    val groupMemberIds: List<String> = emptyList(),
    val timestampMs: Long = System.currentTimeMillis()
)

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

enum class MainDestination {
    HOME,
    FEED,
    KHEDMOT,
    ABEDON,
    MADRASA,
    PROFILE,
    BOOKMARKS,
    VERIFIED_MADRASAS,
    PUBLISH_CIRCULAR,
    PUBLISH_APPLICATION,
    REGISTER_MADRASA,
    MESSAGES,
    NOTIFICATIONS,
    ADMIN_PANEL,
    PRAYER_SCHEDULE,
    AJKER_SMORON,
    APP_SETTINGS,
    ABOUT_APP,
    ABOUT_DEVELOPER,
    PRIVACY_AND_TERMS,
    HELP_AND_SUPPORT
}

enum class FeedFilterTab {
    ALL,            // সব পোস্ট
    JOB_CIRCULAR,   // নিয়োগ বিজ্ঞপ্তি
    APPLICATIONS,   // আবেদন
    GENERAL_POSTS,  // দ্বীনি আলোচনা
    DISTRICT        // জেলা
}

@OptIn(ExperimentalCoroutinesApi::class)
class QawmiViewModel(
    private val repository: QawmiRepository,
    val currentUserId: String,
    val currentUserEmail: String,
    val currentUserDisplayName: String
) : ViewModel() {

    private val _currentDestination = MutableStateFlow(MainDestination.HOME)
    val currentDestination: StateFlow<MainDestination> = _currentDestination.asStateFlow()

    private val _navigationStack = ArrayDeque<MainDestination>()

    private val _selectedFeedFilter = MutableStateFlow(FeedFilterTab.ALL)
    val selectedFeedFilter: StateFlow<FeedFilterTab> = _selectedFeedFilter.asStateFlow()

    private val _selectedDistrictFilter = MutableStateFlow("সকল জেলা")
    val selectedDistrictFilter: StateFlow<String> = _selectedDistrictFilter.asStateFlow()

    private val _selectedKhedmotCategoryFilter = MutableStateFlow("সকল বিভাগ")
    val selectedKhedmotCategoryFilter: StateFlow<String> = _selectedKhedmotCategoryFilter.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _feedbackBanner = MutableStateFlow<String?>(null)
    val feedbackBanner: StateFlow<String?> = _feedbackBanner.asStateFlow()

    private val _errorBanner = MutableStateFlow<String?>(null)
    val errorBanner: StateFlow<String?> = _errorBanner.asStateFlow()

    private val _isSubmitting = MutableStateFlow(false)
    val isSubmitting: StateFlow<Boolean> = _isSubmitting.asStateFlow()

    private val _failedMessageDraft = MutableStateFlow<FailedMessageDraft?>(null)
    val failedMessageDraft: StateFlow<FailedMessageDraft?> = _failedMessageDraft.asStateFlow()

    fun clearFailedMessageDraft() {
        _failedMessageDraft.value = null
    }

    fun cancelSubmittingIfStuck() {
        _isSubmitting.value = false
    }

    private fun runSubmittingAction(
        timeoutMs: Long = 30_000L,
        onTimeoutOrError: ((Throwable) -> Unit)? = null,
        block: suspend () -> Unit
    ) {
        viewModelScope.launch {
            _isSubmitting.value = true
            clearBanners()
            try {
                withTimeout(timeoutMs) {
                    block()
                }
            } catch (e: TimeoutCancellationException) {
                Log.w(TAG, "Submitting action timed out after ${timeoutMs}ms", e)
                _errorBanner.value = "অনুরোধটি সম্পন্ন হতে অতিরিক্ত সময় নিচ্ছে। অনুগ্রহ করে ইন্টারনেট সংযোগ পরীক্ষা করে পুনরায় চেষ্টা করুন।"
                onTimeoutOrError?.invoke(e)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                Log.e(TAG, "Unexpected error in submitting action", e)
                _errorBanner.value = formatFirestoreErrorBangla(e, "কার্যক্রমটি সম্পন্ন করা যায়নি। অনুগ্রহ করে পুনরায় চেষ্টা করুন।")
                onTimeoutOrError?.invoke(e)
            } finally {
                _isSubmitting.value = false
            }
        }
    }

    // Active dialog / detail targets
    private val _selectedPostForApply = MutableStateFlow<FeedPost?>(null)
    val selectedPostForApply: StateFlow<FeedPost?> = _selectedPostForApply.asStateFlow()

    private val _selectedPostForDetail = MutableStateFlow<FeedPost?>(null)
    val selectedPostForDetail: StateFlow<FeedPost?> = _selectedPostForDetail.asStateFlow()

    private val _messageTargetUser = MutableStateFlow<Pair<String, String>?>(null) // (uid, name)
    val messageTargetUser: StateFlow<Pair<String, String>?> = _messageTargetUser.asStateFlow()

    // Public profile inspection target (uid to fallbackName)
    private val _inspectedUserTarget = MutableStateFlow<Pair<String, String>?>(null)
    val inspectedUserTarget: StateFlow<Pair<String, String>?> = _inspectedUserTarget.asStateFlow()

    // Focused "আজকের স্মরণ" reminder ID when opened via push notification deep-link
    private val _focusedSmoronId = MutableStateFlow<String?>(null)
    val focusedSmoronId: StateFlow<String?> = _focusedSmoronId.asStateFlow()

    val smoronServerReceiptsState: StateFlow<Map<String, Long>> = repository.observeSmoronServerReceipts(currentUserId)
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyMap())

    // Auto-Update System State (সর্বশেষ ভার্সন ও ডাউনলোড লিংক পরিচালনা)
    val latestAppReleaseState: StateFlow<AppReleaseInfo?> = repository.observeLatestAppRelease()
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val _dismissedUpdateVersionCode = MutableStateFlow<Int?>(null)
    val dismissedUpdateVersionCode: StateFlow<Int?> = _dismissedUpdateVersionCode.asStateFlow()

    fun dismissCurrentUpdatePrompt(versionCode: Int) {
        _dismissedUpdateVersionCode.value = versionCode
    }

    fun resetDismissedUpdatePrompt() {
        _dismissedUpdateVersionCode.value = null
    }

    // Server-side Admin Registry & Token Claim state
    private val _serverTokenVerifiedAdmin = MutableStateFlow(
        AdminSecurityHelper.isAuthorizedAdminEmail(currentUserEmail)
    )

    val adminRegistryEntryState: StateFlow<AdminRegistryEntry?> = repository.observeAdminRegistryEntry(currentUserId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), null)

    val isAdminState: StateFlow<Boolean> = combine(
        _serverTokenVerifiedAdmin,
        adminRegistryEntryState
    ) { tokenVerified, registryEntry ->
        AdminSecurityHelper.isFirebaseAuthUserAdmin(
            authEmail = currentUserEmail,
            hasCustomAdminClaim = tokenVerified,
            hasServerRegistryEntry = registryEntry?.verifiedByAuthToken == true
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        AdminSecurityHelper.isAuthorizedAdminEmail(currentUserEmail)
    )

    val isAdminUser: Boolean
        get() = isAdminState.value

    // Real-time Firestore StateFlows (Two-tier error handling)
    val userProfileState: StateFlow<UiState<UserProfile?>> = repository.observeUserProfile(currentUserId)
        .map<UserProfile?, UiState<UserProfile?>> { UiState.Success(it) }
        .catch { e ->
            Log.w(TAG, "Error observing user profile", e)
            emit(UiState.Error(formatFirestoreErrorBangla(e, "প্রোফাইল লোড করা যায়নি")))
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, UiState.Loading)

    val inspectedUserProfileState: StateFlow<UiState<UserProfile?>> = _inspectedUserTarget
        .flatMapLatest { target ->
            if (target == null || target.first.isBlank()) {
                flowOf(UiState.Success(null))
            } else {
                repository.observeUserProfile(target.first)
                    .map<UserProfile?, UiState<UserProfile?>> { UiState.Success(it) }
                    .catch { e ->
                        Log.w(TAG, "Error observing inspected user profile", e)
                        emit(UiState.Error(formatFirestoreErrorBangla(e, "ব্যবহারকারীর প্রোফাইল লোড করা যায়নি")))
                    }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    private data class PostEngagementOverride(
        val userReactions: Map<String, String>? = null,
        val userReactionNames: Map<String, String>? = null,
        val commentCountDelta: Int = 0,
        val minExpectedShareCount: Int = 0
    )

    private val _adminDataRefreshToken = MutableStateFlow(0)
    private val _localOptimisticPosts = MutableStateFlow<List<FeedPost>>(emptyList())
    private val _localPostEngagementOverrides = MutableStateFlow<Map<String, PostEngagementOverride>>(emptyMap())
    private val _localOptimisticComments = MutableStateFlow<List<FeedComment>>(emptyList())
    private val _locallyDeletedCommentIds = MutableStateFlow<Set<String>>(emptySet())
    private val _localOptimisticSubmissions = MutableStateFlow<List<KhedmotSubmission>>(emptyList())
    private val _localOptimisticMessages = MutableStateFlow<List<DirectMessage>>(emptyList())

    val feedPostsState: StateFlow<UiState<List<FeedPost>>> = combine(
        _adminDataRefreshToken.flatMapLatest {
            repository.observePublicFeedPosts()
        },
        _localOptimisticPosts,
        _localPostEngagementOverrides
    ) { remotePosts, optimisticPosts, engagementOverrides ->
        val remoteIds = remotePosts.map { it.id }.toSet()
        val pendingOptimistic = optimisticPosts.filter { it.id.isNotBlank() && it.id !in remoteIds }
        val merged = (pendingOptimistic + remotePosts)
            .distinctBy { it.id }
            .map { post ->
                val override = engagementOverrides[post.id]
                if (override == null) {
                    post
                } else {
                    val mergedReactions = if (override.userReactions != null && currentUserId.isNotBlank()) {
                        val baseMap = post.userReactions.toMutableMap()
                        val myOverrideReaction = override.userReactions[currentUserId]
                        if (myOverrideReaction.isNullOrBlank()) {
                            baseMap.remove(currentUserId)
                        } else {
                            baseMap[currentUserId] = myOverrideReaction
                        }
                        baseMap
                    } else {
                        post.userReactions
                    }
                    val mergedNames = if (override.userReactionNames != null && currentUserId.isNotBlank()) {
                        val baseNames = post.userReactionNames.toMutableMap()
                        val myOverrideName = override.userReactionNames[currentUserId]
                        if (myOverrideName.isNullOrBlank()) {
                            baseNames.remove(currentUserId)
                        } else {
                            baseNames[currentUserId] = myOverrideName
                        }
                        baseNames
                    } else {
                        post.userReactionNames
                    }
                    post.copy(
                        userReactions = mergedReactions,
                        userReactionNames = mergedNames,
                        commentCount = (post.commentCount + override.commentCountDelta).coerceAtLeast(0),
                        shareCount = maxOf(post.shareCount, override.minExpectedShareCount)
                    )
                }
            }
            .sortedWith(
                compareByDescending<FeedPost> { it.isBoosted }
                    .thenByDescending { it.createdAt ?: Timestamp(0, 0) }
            )
        UiState.Success(merged) as UiState<List<FeedPost>>
    }
        .catch { e ->
            Log.w(TAG, "Error observing feed posts", e)
            val fallback = _localOptimisticPosts.value
            if (fallback.isNotEmpty()) {
                emit(UiState.Success(fallback))
            } else {
                emit(UiState.Error(formatFirestoreErrorBangla(e, "ফিড লোড করা যায়নি")))
            }
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, UiState.Loading)

    val postCommentsState: StateFlow<UiState<List<FeedComment>>> = combine(
        _adminDataRefreshToken.flatMapLatest {
            repository.observePublicPostComments()
        },
        _localOptimisticComments,
        _locallyDeletedCommentIds
    ) { remoteComments, optimisticComments, deletedIds ->
        val optimisticById = optimisticComments.associateBy { it.id }
        val remoteIds = remoteComments.map { it.id }.toSet()
        val pendingOptimistic = optimisticComments.filter { it.id.isNotBlank() && it.id !in remoteIds && it.id !in deletedIds }
        val merged = (remoteComments.map { remote -> optimisticById[remote.id] ?: remote } + pendingOptimistic)
            .filter { it.id.isNotBlank() && it.id !in deletedIds }
            .distinctBy { it.id }
            .sortedWith(
                compareBy<FeedComment> { it.createdAt ?: Timestamp(0, 0) }
                    .thenBy { it.id }
            )
        UiState.Success(merged) as UiState<List<FeedComment>>
    }
        .catch { e ->
            Log.w(TAG, "Error observing post comments", e)
            val fallback = _localOptimisticComments.value.filterNot { it.id in _locallyDeletedCommentIds.value }
            if (fallback.isNotEmpty()) {
                emit(UiState.Success(fallback))
            } else {
                emit(UiState.Error(formatFirestoreErrorBangla(e, "কমেন্ট লোড করা যায়নি")))
            }
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, UiState.Loading)

    val bookmarksState: StateFlow<UiState<List<BookmarkItem>>> = _adminDataRefreshToken
        .flatMapLatest {
            repository.observeBookmarks(currentUserId)
                .map<List<BookmarkItem>, UiState<List<BookmarkItem>>> { UiState.Success(it) }
                .catch { e ->
                    Log.w(TAG, "Error observing bookmarks", e)
                    emit(UiState.Error(formatFirestoreErrorBangla(e, "পছন্দের খেদমত লোড করা যায়নি")))
                }
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, UiState.Loading)

    val madrasasState: StateFlow<UiState<List<MadrasaProfile>>> = _adminDataRefreshToken
        .flatMapLatest {
            repository.observePublicMadrasas()
                .map<List<MadrasaProfile>, UiState<List<MadrasaProfile>>> { UiState.Success(it) }
                .catch { e ->
                    Log.w(TAG, "Error observing madrasas", e)
                    emit(UiState.Error(formatFirestoreErrorBangla(e, "মাদরাসার তালিকা লোড করা যায়নি")))
                }
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, UiState.Loading)

    val mySentSubmissionsState: StateFlow<UiState<List<KhedmotSubmission>>> = combine(
        _adminDataRefreshToken.flatMapLatest {
            repository.observeMySentSubmissions(currentUserId)
        },
        _localOptimisticSubmissions
    ) { remoteList, optimisticList ->
        val remoteIds = remoteList.map { it.id }.toSet()
        val pending = optimisticList.filter { it.id.isNotBlank() && it.id !in remoteIds }
        val merged = (pending + remoteList)
            .distinctBy { it.id }
            .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
        UiState.Success(merged) as UiState<List<KhedmotSubmission>>
    }
        .catch { e ->
            Log.w(TAG, "Error observing sent submissions", e)
            val fallback = _localOptimisticSubmissions.value
            if (fallback.isNotEmpty()) {
                emit(UiState.Success(fallback))
            } else {
                emit(UiState.Error(formatFirestoreErrorBangla(e, "আবেদন তালিকা লোড করা যায়নি")))
            }
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, UiState.Loading)

    val receivedSubmissionsState: StateFlow<UiState<List<KhedmotSubmission>>> = _adminDataRefreshToken
        .flatMapLatest {
            repository.observeReceivedSubmissions(currentUserId)
                .map<List<KhedmotSubmission>, UiState<List<KhedmotSubmission>>> { UiState.Success(it) }
                .catch { e ->
                    Log.w(TAG, "Error observing received submissions", e)
                    emit(UiState.Error(formatFirestoreErrorBangla(e, "প্রাপ্ত আবেদন লোড করা যায়নি")))
                }
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, UiState.Loading)

    // Optimistic local read message IDs so unread badge decrements/clears immediately while Firestore batch commits
    private val _locallyReadMessageIds = MutableStateFlow<Set<String>>(emptySet())

    val userGroupsState: StateFlow<UiState<List<ChatGroup>>> = _adminDataRefreshToken
        .flatMapLatest {
            repository.observeUserGroups(currentUserId)
                .map<List<ChatGroup>, UiState<List<ChatGroup>>> { UiState.Success(it) }
                .catch { e ->
                    Log.w(TAG, "Error observing chat groups", e)
                    emit(UiState.Error(formatFirestoreErrorBangla(e, "গ্রুপ লোড করা যায়নি")))
                }
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, UiState.Loading)

    val messagesState: StateFlow<UiState<List<DirectMessage>>> = combine(
        _adminDataRefreshToken.flatMapLatest {
            repository.observeUserMessages(currentUserId)
        },
        _locallyReadMessageIds,
        _localOptimisticMessages
    ) { list, locallyReadIds, optimisticMsgs ->
        val remoteIds = list.map { it.id }.toSet()
        val optimisticById = optimisticMsgs.associateBy { it.id }
        val pendingOptimistic = optimisticMsgs.filter { it.id.isNotBlank() && it.id !in remoteIds }
        val combinedList = pendingOptimistic + list
        val deduplicated = combinedList
            .filter { it.id.isNotBlank() }
            .filterNot { msg ->
                (msg.senderId == currentUserId && msg.deletedForSender) ||
                    (msg.receiverId == currentUserId && msg.deletedForReceiver) ||
                    msg.deletedForUserIds.contains(currentUserId)
            }
            .distinctBy { it.id }
            .map { msg ->
                val localOverrideState = optimisticById[msg.id]?.localDeliveryState.orEmpty()
                val isDeliveredOrSeenRemotely = msg.id in remoteIds && (
                    msg.isRead ||
                        msg.readByUserIds.any { it.isNotBlank() && it != currentUserId }
                    )
                val isActivelyInFlight = msg.id in inFlightMessageIds
                val isConfirmedRemotely = msg.id in remoteIds && !isActivelyInFlight && localOverrideState != "FAILED"
                val resolvedState = when {
                    isDeliveredOrSeenRemotely || localOverrideState == "SENT" || isConfirmedRemotely -> "SENT"
                    localOverrideState == "FAILED" -> "FAILED"
                    localOverrideState == "SENDING" || isActivelyInFlight -> "SENDING"
                    else -> "SENT"
                }
                if (locallyReadIds.contains(msg.id)) {
                    msg.copy(
                        isRead = true,
                        readByUserIds = (msg.readByUserIds + currentUserId).distinct()
                    ).apply {
                        localDeliveryState = resolvedState
                    }
                } else {
                    msg.copy().apply {
                        localDeliveryState = resolvedState
                    }
                }
            }
            .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
        UiState.Success(deduplicated) as UiState<List<DirectMessage>>
    }
        .catch { e ->
            Log.w(TAG, "Error observing messages", e)
            val fallback = _localOptimisticMessages.value
            if (fallback.isNotEmpty()) {
                emit(UiState.Success(fallback))
            } else {
                emit(UiState.Error(formatFirestoreErrorBangla(e, "মেসেজ লোড করা যায়নি")))
            }
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, UiState.Loading)

    // Real-time profiles of all conversation partners in the inbox (for Online/Offline, Last Seen & Avatar)
    val chatPartnerProfilesState: StateFlow<Map<String, UserProfile>> = combine(
        messagesState,
        userGroupsState,
        _messageTargetUser
    ) { msgState, groupsState, targetUser ->
        val messages = (msgState as? UiState.Success)?.data ?: emptyList()
        val groups = (groupsState as? UiState.Success)?.data ?: emptyList()
        val partnerIds = messages.flatMap { msg ->
            listOf(msg.senderId, msg.receiverId)
        }.toMutableSet()
        groups.forEach { group ->
            partnerIds.addAll(group.memberIds)
        }
        targetUser?.first?.takeIf { it.isNotBlank() && !it.startsWith("GROUP:") }?.let { partnerIds.add(it) }
        partnerIds
            .filter { it.isNotBlank() && it != currentUserId && !it.startsWith("GROUP:") }
            .sorted()
            .take(30)
    }
        .distinctUntilChanged()
        .flatMapLatest { validIds ->
            if (validIds.isEmpty()) {
                flowOf(emptyMap())
            } else {
                val flows = validIds.map { partnerUid ->
                    repository.observeUserProfile(partnerUid)
                        .map { profile -> partnerUid to profile }
                        .catch { emit(partnerUid to null) }
                }
                combine(flows) { pairs ->
                    pairs.mapNotNull { (uid, profile) ->
                        if (profile != null) uid to profile else null
                    }.toMap()
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyMap())

    val activeChatPartnerProfileState: StateFlow<UserProfile?> = _messageTargetUser
        .map { it?.first.orEmpty() }
        .distinctUntilChanged()
        .flatMapLatest { uid ->
            if (uid.isBlank() || uid == currentUserId || uid.startsWith("GROUP:")) {
                flowOf(null)
            } else {
                repository.observeUserProfile(uid)
                    .catch { emit(null) }
            }
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val notificationsState: StateFlow<UiState<List<AppNotification>>> = _adminDataRefreshToken
        .flatMapLatest {
            repository.observeNotifications(currentUserId)
                .map<List<AppNotification>, UiState<List<AppNotification>>> { UiState.Success(it) }
                .catch { e ->
                    Log.w(TAG, "Error observing notifications", e)
                    emit(UiState.Error(formatFirestoreErrorBangla(e, "নোটিফিকেশন লোড করা যায়নি")))
                }
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, UiState.Loading)

    // Real-time Telegram notifications queue (strictly observed ONLY when verified Admin is logged in)
    val telegramNotificationsState: StateFlow<UiState<List<TelegramNotificationLog>>> = combine(
        isAdminState,
        _adminDataRefreshToken
    ) { isAdmin, token -> isAdmin to token }
        .flatMapLatest { (isAdmin, _) ->
            if (!isAdmin) {
                flowOf(UiState.Success(emptyList()))
            } else {
                repository.observeTelegramNotifications()
                    .map<List<TelegramNotificationLog>, UiState<List<TelegramNotificationLog>>> { UiState.Success(it) }
                    .catch { e ->
                        Log.w(TAG, "Error observing Telegram notifications", e)
                        emit(UiState.Error(formatFirestoreErrorBangla(e, "টেলিগ্রাম নোটিফিকেশন লগ লোড করা যায়নি")))
                    }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val adminUsersState: StateFlow<UiState<List<UserProfile>>> = combine(
        isAdminState,
        _adminDataRefreshToken
    ) { isAdmin, token -> isAdmin to token }
        .flatMapLatest { (isAdmin, _) ->
            if (!isAdmin) {
                flowOf(UiState.Success(emptyList()))
            } else {
                repository.observeAllUsersForAdmin()
                    .map<List<UserProfile>, UiState<List<UserProfile>>> { UiState.Success(it) }
                    .catch { e ->
                        Log.w(TAG, "Error observing admin users", e)
                        emit(UiState.Error(formatFirestoreErrorBangla(e, "ব্যবহারকারীর তালিকা লোড করা যায়নি")))
                    }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val adminSubmissionsState: StateFlow<UiState<List<KhedmotSubmission>>> = combine(
        isAdminState,
        _adminDataRefreshToken
    ) { isAdmin, token -> isAdmin to token }
        .flatMapLatest { (isAdmin, _) ->
            if (!isAdmin) {
                flowOf(UiState.Success(emptyList()))
            } else {
                repository.observeAllSubmissionsForAdmin()
                    .map<List<KhedmotSubmission>, UiState<List<KhedmotSubmission>>> { UiState.Success(it) }
                    .catch { e ->
                        Log.w(TAG, "Error observing admin submissions", e)
                        emit(UiState.Error(formatFirestoreErrorBangla(e, "আবেদনের তালিকা লোড করা যায়নি")))
                    }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val adminRegistryListState: StateFlow<UiState<List<AdminRegistryEntry>>> = combine(
        isAdminState,
        _adminDataRefreshToken
    ) { isAdmin, token -> isAdmin to token }
        .flatMapLatest { (isAdmin, _) ->
            if (!isAdmin) {
                flowOf(UiState.Success(emptyList()))
            } else {
                repository.observeAllAdminsForAdmin()
                    .map<List<AdminRegistryEntry>, UiState<List<AdminRegistryEntry>>> { UiState.Success(it) }
                    .catch { e ->
                        Log.w(TAG, "Error observing admin registry list", e)
                        emit(UiState.Error(formatFirestoreErrorBangla(e, "অ্যাডমিন তালিকা লোড করা যায়নি")))
                    }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    private val acknowledgedDeliveredIds = java.util.Collections.synchronizedSet(mutableSetOf<String>())
    private val inFlightMessageIds = java.util.Collections.synchronizedSet(mutableSetOf<String>())
    private val recoveredPendingSyncIds = java.util.Collections.synchronizedSet(mutableSetOf<String>())
    private val pendingOutgoingQueue = java.util.concurrent.ConcurrentHashMap<String, FailedMessageDraft>()

    init {
        setUserOnlineStatus(true)
        verifyAndSyncAdminIdentity()
        observeAndAcknowledgeDeliveredMessages()
    }

    private fun observeAndAcknowledgeDeliveredMessages() {
        if (currentUserId.isBlank()) return
        viewModelScope.launch {
            val deliveredToken = "DELIVERED:$currentUserId"
            messagesState.collect { state ->
                val list = (state as? UiState.Success)?.data ?: return@collect

                // 1. Clean up local optimistic entries that are now confirmed and no longer in-flight
                val confirmedOwnIds = list.filter { msg ->
                    msg.senderId == currentUserId &&
                        msg.localDeliveryState == "SENT" &&
                        msg.id !in inFlightMessageIds
                }.map { it.id }.toSet()
                if (confirmedOwnIds.isNotEmpty()) {
                    confirmedOwnIds.forEach { confirmedId ->
                        pendingOutgoingQueue.remove(confirmedId)
                        if (_failedMessageDraft.value?.messageId == confirmedId) {
                            _failedMessageDraft.value = null
                        }
                    }
                    if (_localOptimisticMessages.value.any { it.id in confirmedOwnIds }) {
                        _localOptimisticMessages.value = _localOptimisticMessages.value.filterNot { it.id in confirmedOwnIds }
                    }
                }

                // 2. Acknowledge incoming unread 1-on-1 and Group messages so sender sees Delivered (✓✓) in real time
                val unacknowledged = list.filter { msg ->
                    val isGroupMsg = msg.groupId.isNotBlank() || msg.receiverId.startsWith("GROUP:", ignoreCase = true)
                    val isEligibleRecipient = if (isGroupMsg) {
                        currentUserId in msg.groupMemberIds
                    } else {
                        msg.receiverId == currentUserId
                    }
                    msg.id.isNotBlank() &&
                        msg.id !in acknowledgedDeliveredIds &&
                        msg.senderId != currentUserId &&
                        isEligibleRecipient &&
                        !msg.isRead &&
                        !msg.readByUserIds.contains(currentUserId) &&
                        !msg.readByUserIds.contains(deliveredToken)
                }
                if (unacknowledged.isNotEmpty()) {
                    unacknowledged.forEach { acknowledgedDeliveredIds.add(it.id) }
                    repository.acknowledgeMessagesDelivered(unacknowledged)
                }
            }
        }
    }

    private fun verifyAndSyncAdminIdentity() {
        viewModelScope.launch {
            val result = repository.ensureVerifiedAdminRegistryDocument()
            result.onSuccess { isVerifiedAdmin ->
                _serverTokenVerifiedAdmin.value = isVerifiedAdmin
            }
        }
    }

    fun setUserOnlineStatus(isOnline: Boolean) {
        viewModelScope.launch {
            repository.updateUserPresence(
                isOnline = isOnline,
                defaultFullName = resolveActorDisplayName("সম্মানিত দ্বীনি ভাই")
            )
        }
    }

    private fun resolveActorDisplayName(fallback: String = "কওমি কর্মসংস্থান"): String {
        return (userProfileState.value as? UiState.Success)?.data?.fullName
            ?.takeIf { it.isNotBlank() }
            ?: currentUserDisplayName.ifBlank { fallback }
    }

    private fun resolveActorPhotoUrl(): String {
        return (userProfileState.value as? UiState.Success)?.data?.photoUrl.orEmpty()
    }

    fun navigateTo(destination: MainDestination) {
        if (destination == MainDestination.ADMIN_PANEL && !isAdminUser) {
            _errorBanner.value = "অনুমোদনহীন প্রবেশাধিকার: শুধুমাত্র যাচাইকৃত অ্যাডমিন অ্যাকাউন্ট দিয়ে অ্যাডমিন প্যানেল ব্যবহার করা যাবে।"
            return
        }
        if (_currentDestination.value != destination) {
            clearBanners()
            _navigationStack.addLast(_currentDestination.value)
            _currentDestination.value = destination
        }
    }

    fun navigateBottomTab(destination: MainDestination) {
        if (_currentDestination.value != destination) {
            clearBanners()
        }
        _navigationStack.clear()
        _currentDestination.value = destination
    }

    fun navigateBack(): Boolean {
        return if (_navigationStack.isNotEmpty()) {
            clearBanners()
            _currentDestination.value = _navigationStack.removeLast()
            true
        } else if (_currentDestination.value != MainDestination.HOME) {
            clearBanners()
            _currentDestination.value = MainDestination.HOME
            true
        } else {
            false
        }
    }

    fun setFeedFilter(tab: FeedFilterTab) {
        _selectedFeedFilter.value = tab
    }

    fun setDistrictFilter(district: String) {
        _selectedDistrictFilter.value = district
    }

    fun setKhedmotCategoryFilter(category: String) {
        _selectedKhedmotCategoryFilter.value = category
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun openFeedWithFilter(tab: FeedFilterTab) {
        _selectedFeedFilter.value = tab
        navigateTo(MainDestination.FEED)
    }

    fun retryFeedStreams() {
        clearBanners()
        _adminDataRefreshToken.value += 1
    }

    fun openApplyDialog(post: FeedPost?) {
        _selectedPostForApply.value = post
    }

    fun openPostDetailDialog(post: FeedPost?) {
        _selectedPostForDetail.value = post
    }

    fun openUserProfileInspection(userId: String, fallbackName: String) {
        if (userId.isBlank()) return
        if (userId == currentUserId) {
            _inspectedUserTarget.value = null
            navigateTo(MainDestination.PROFILE)
        } else {
            _inspectedUserTarget.value = userId to fallbackName
        }
    }

    fun closeUserProfileInspection() {
        _inspectedUserTarget.value = null
    }

    fun openDirectMessageTo(userId: String, userName: String) {
        if (userId.isBlank() || userId == currentUserId) return
        _inspectedUserTarget.value = null
        _messageTargetUser.value = userId to userName
        navigateTo(MainDestination.MESSAGES)
        markConversationRead(userId)
    }

    fun clearMessageTarget() {
        val previousTarget = _messageTargetUser.value?.first.orEmpty()
        _messageTargetUser.value = null
        if (previousTarget.isNotBlank() && !previousTarget.startsWith("GROUP:")) {
            setTypingStatus(previousTarget, false)
        }
    }

    private var lastTypingTargetId: String = ""
    private var lastTypingState: Boolean = false
    private var lastTypingSentAtMs: Long = 0L

    fun setTypingStatus(targetUserId: String, isTyping: Boolean) {
        val now = System.currentTimeMillis()
        if (isTyping && targetUserId == lastTypingTargetId && lastTypingState && (now - lastTypingSentAtMs) < 4_000L) {
            return
        }
        if (!isTyping && !lastTypingState && targetUserId == lastTypingTargetId) {
            return
        }
        lastTypingTargetId = if (isTyping) targetUserId else ""
        lastTypingState = isTyping
        lastTypingSentAtMs = now
        viewModelScope.launch {
            repository.updateTypingStatus(
                targetUserId = targetUserId,
                isTyping = isTyping,
                defaultFullName = resolveActorDisplayName("সম্মানিত দ্বীনি ভাই")
            )
        }
    }

    fun clearBanners() {
        _feedbackBanner.value = null
        _errorBanner.value = null
    }

    fun saveProfile(
        fullName: String,
        accountType: String,
        district: String,
        designation: String,
        qualification: String,
        experience: String,
        preferredDistricts: String,
        bio: String,
        contactInfo: String,
        madrasaName: String,
        photoUrl: String = "",
        onComplete: () -> Unit = {}
    ) {
        if (fullName.isBlank()) {
            _errorBanner.value = "অনুগ্রহ করে আপনার পূর্ণ নাম উল্লেখ করুন।"
            return
        }
        runSubmittingAction {
            val existingPhoto = (userProfileState.value as? UiState.Success)?.data?.photoUrl.orEmpty()
            val profile = UserProfile(
                userId = currentUserId,
                fullName = fullName.trim(),
                accountType = accountType,
                district = district.trim(),
                designation = designation.trim(),
                qualification = qualification.trim(),
                experience = experience.trim(),
                preferredDistricts = preferredDistricts.trim(),
                bio = bio.trim(),
                contactInfo = contactInfo.trim(),
                madrasaName = madrasaName.trim(),
                photoUrl = photoUrl.ifBlank { existingPhoto },
                isOnline = true
            )
            val result = repository.saveUserProfile(profile)
            result.onSuccess {
                _feedbackBanner.value = "আলহামদুলিল্লাহ, আপনার প্রোফাইল তথ্য ও ছবি সংরক্ষিত হয়েছে।"
                onComplete()
            }.onFailure { err ->
                _errorBanner.value = err.localizedMessage ?: "প্রোফাইল সংরক্ষণে ত্রুটি হয়েছে।"
            }
        }
    }

    fun updateProfilePhotoImmediately(photoUrl: String) {
        runSubmittingAction {
            val fallbackName = resolveActorDisplayName("আবদুল্লাহ")
            val result = repository.updateProfilePhoto(photoUrl, fallbackName)
            result.onSuccess {
                _feedbackBanner.value = "আলহামদুলিল্লাহ, আপনার প্রোফাইল ছবি সফলভাবে সংরক্ষিত হয়েছে।"
            }.onFailure { err ->
                _errorBanner.value = err.localizedMessage ?: "প্রোফাইল ছবি সংরক্ষণে ত্রুটি হয়েছে।"
            }
        }
    }

    fun publishFeedPost(
        authorName: String,
        postType: String,
        title: String,
        district: String,
        upazilaOrLocation: String,
        designation: String,
        qualification: String,
        salaryOrExperience: String,
        deadlineOrPreferredDistricts: String,
        description: String,
        contactNumber: String,
        khedmotCategories: List<String> = emptyList(),
        customCategory: String = "",
        imageUrls: List<String> = emptyList(),
        onSuccessNavigate: () -> Unit = {}
    ) {
        if (KhedmotCategoryHelper.CAT_OTHER in khedmotCategories && customCategory.trim().isBlank()) {
            _errorBanner.value = "‘অন্যান্য’ বিভাগ নির্বাচন করলে অনুগ্রহ করে আপনার খেদমতের ধরনটি লিখুন।"
            return
        }
        val effectiveDesignation = KhedmotCategoryHelper.buildCombinedDesignation(
            manualDesignation = designation,
            selectedCategories = khedmotCategories,
            customCategory = customCategory
        )
        if (postType != "GENERAL_POST" && (title.isBlank() || district.isBlank() || effectiveDesignation.isBlank() || qualification.isBlank())) {
            _errorBanner.value = "অনুগ্রহ করে শিরোনাম, জেলা, খেদমতের বিভাগ/পদ এবং শিক্ষাগত যোগ্যতা পূরণ করুন।"
            return
        }
        if (postType == "GENERAL_POST" && title.isBlank() && description.isBlank() && imageUrls.isEmpty()) {
            _errorBanner.value = "অনুগ্রহ করে পোস্টের শিরোনাম বা বিবরণ লিখুন।"
            return
        }
        runSubmittingAction(timeoutMs = 35_000L) {
            val resolvedTitle = title.trim().ifBlank {
                description.trim().lineSequence().firstOrNull()?.take(80)?.ifBlank { "দ্বীনি খেদমত ও আলোচনা" }
                    ?: "দ্বীনি খেদমত ও আলোচনা"
            }
            val resolvedAuthorName = authorName.ifBlank { resolveActorDisplayName(resolvedTitle) }
            val resolvedDistrict = district.trim().ifBlank {
                (userProfileState.value as? UiState.Success)?.data?.district.orEmpty()
            }
            val resolvedUpazila = upazilaOrLocation.ifBlank { resolvedDistrict }
            val resolvedDesignation = effectiveDesignation.ifBlank {
                if (postType == "GENERAL_POST") "দ্বীনি আলোচনা" else designation
            }
            val resolvedSalary = salaryOrExperience.ifBlank { if (postType == "GENERAL_POST") "" else "আলোচনা সাপেক্ষ" }
            val resolvedDeadline = deadlineOrPreferredDistricts.ifBlank { if (postType == "GENERAL_POST") "" else "চলমান" }
            val resolvedPhoto = resolveActorPhotoUrl()

            val result = repository.createFeedPost(
                authorName = resolvedAuthorName,
                postType = postType,
                title = resolvedTitle,
                district = resolvedDistrict,
                upazilaOrLocation = resolvedUpazila,
                designation = resolvedDesignation,
                qualification = qualification,
                salaryOrExperience = resolvedSalary,
                deadlineOrPreferredDistricts = resolvedDeadline,
                description = description,
                contactNumber = contactNumber,
                authorPhotoUrl = resolvedPhoto,
                khedmotCategories = khedmotCategories,
                customCategory = customCategory,
                imageUrls = imageUrls
            )
            result.onSuccess { postId ->
                if (postId.isNotBlank()) {
                    val optimisticPost = FeedPost(
                        id = postId,
                        authorId = currentUserId,
                        authorName = resolvedAuthorName,
                        authorPhotoUrl = resolvedPhoto,
                        postType = postType,
                        title = resolvedTitle,
                        district = resolvedDistrict,
                        upazilaOrLocation = resolvedUpazila,
                        designation = resolvedDesignation,
                        khedmotCategories = khedmotCategories,
                        customCategory = customCategory,
                        qualification = qualification,
                        salaryOrExperience = resolvedSalary,
                        deadlineOrPreferredDistricts = resolvedDeadline,
                        description = description,
                        contactNumber = contactNumber,
                        imageUrls = imageUrls,
                        visibility = "PUBLIC",
                        createdAt = Timestamp.now(),
                        updatedAt = Timestamp.now()
                    )
                    _localOptimisticPosts.value = (listOf(optimisticPost) + _localOptimisticPosts.value).distinctBy { it.id }
                }
                _feedbackBanner.value = when (postType) {
                    "JOB_CIRCULAR" -> "আলহামদুলিল্লাহ, খেদমতের নিয়োগ বিজ্ঞপ্তিটি ফিডে প্রকাশিত হয়েছে।"
                    "GENERAL_POST" -> "আলহামদুলিল্লাহ, আপনার দ্বীনি পোস্টটি ফিডে প্রকাশিত হয়েছে।"
                    else -> "আলহামদুলিল্লাহ, আপনার খেদমতের আবেদনটি ফিডে প্রকাশিত হয়েছে।"
                }
                onSuccessNavigate()
            }.onFailure { err ->
                _errorBanner.value = err.localizedMessage ?: "প্রকাশ করতে সমস্যা হয়েছে।"
            }
        }
    }

    fun updateOwnPost(
        post: FeedPost,
        title: String,
        district: String,
        upazilaOrLocation: String,
        designation: String,
        qualification: String,
        salaryOrExperience: String,
        deadlineOrPreferredDistricts: String,
        description: String,
        contactNumber: String,
        khedmotCategories: List<String> = post.khedmotCategories,
        customCategory: String = post.customCategory,
        imageUrls: List<String> = post.imageUrls
    ) {
        if (KhedmotCategoryHelper.CAT_OTHER in khedmotCategories && customCategory.trim().isBlank()) {
            _errorBanner.value = "‘অন্যান্য’ বিভাগ নির্বাচন করলে অনুগ্রহ করে খেদমতের ধরনটি লিখুন।"
            return
        }
        runSubmittingAction(timeoutMs = 35_000L) {
            val res = repository.updateOwnFeedPost(
                post = post,
                title = title,
                district = district,
                upazilaOrLocation = upazilaOrLocation,
                designation = designation,
                qualification = qualification,
                salaryOrExperience = salaryOrExperience,
                deadlineOrPreferredDistricts = deadlineOrPreferredDistricts,
                description = description,
                contactNumber = contactNumber,
                khedmotCategories = khedmotCategories,
                customCategory = customCategory,
                imageUrls = imageUrls
            )
            res.onSuccess {
                _localOptimisticPosts.value = _localOptimisticPosts.value.map { existing ->
                    if (existing.id == post.id) {
                        existing.copy(
                            title = title,
                            district = district,
                            upazilaOrLocation = upazilaOrLocation,
                            designation = designation,
                            qualification = qualification,
                            salaryOrExperience = salaryOrExperience,
                            deadlineOrPreferredDistricts = deadlineOrPreferredDistricts,
                            description = description,
                            contactNumber = contactNumber,
                            khedmotCategories = khedmotCategories,
                            customCategory = customCategory,
                            imageUrls = imageUrls,
                            isEdited = true,
                            updatedAt = Timestamp.now()
                        )
                    } else {
                        existing
                    }
                }
                _feedbackBanner.value = "আলহামদুলিল্লাহ, আপনার পোস্টটি সফলভাবে হালনাগাদ হয়েছে।"
            }.onFailure { err ->
                _errorBanner.value = err.localizedMessage ?: "পোস্ট হালনাগাদ করা যায়নি।"
            }
        }
    }

    fun togglePostReaction(post: FeedPost, reactionCode: String) {
        val reactorName = resolveActorDisplayName("সম্মানিত দ্বীনি ভাই").trim().ifBlank { "সম্মানিত সদস্য" }.take(120)
        val previousOverride = _localPostEngagementOverrides.value[post.id]
        val currentReactions = (previousOverride?.userReactions ?: post.userReactions).toMutableMap()
        val currentNames = (previousOverride?.userReactionNames ?: post.userReactionNames).toMutableMap()
        val existingReaction = currentReactions[currentUserId].orEmpty()

        if (reactionCode.isBlank() || existingReaction == reactionCode) {
            currentReactions.remove(currentUserId)
            currentNames.remove(currentUserId)
        } else if (currentUserId.isNotBlank()) {
            currentReactions[currentUserId] = reactionCode
            currentNames[currentUserId] = reactorName
        }

        val updatedOverride = (previousOverride ?: PostEngagementOverride()).copy(
            userReactions = currentReactions,
            userReactionNames = currentNames
        )
        _localPostEngagementOverrides.value = _localPostEngagementOverrides.value + (post.id to updatedOverride)

        viewModelScope.launch {
            val res = repository.togglePostReaction(post, reactionCode, reactorName)
            res.onFailure { err ->
                // Roll back optimistic reaction override on failure
                val rollbackMap = _localPostEngagementOverrides.value.toMutableMap()
                if (previousOverride == null) {
                    rollbackMap.remove(post.id)
                } else {
                    rollbackMap[post.id] = previousOverride
                }
                _localPostEngagementOverrides.value = rollbackMap
                _errorBanner.value = err.localizedMessage ?: "রিঅ্যাকশন যুক্ত করা যায়নি।"
            }
        }
    }

    fun addCommentToPost(
        post: FeedPost,
        text: String,
        parentCommentId: String = "",
        replyToAuthorName: String = ""
    ) {
        val cleanText = text.trim()
        if (cleanText.isBlank()) {
            _errorBanner.value = "অনুগ্রহ করে কমেন্ট লিখুন।"
            return
        }
        val authorName = resolveActorDisplayName("সম্মানিত দ্বীনি ভাই")
        val authorPhoto = ProfilePhotoHelper.compactSenderPhotoForMessage(resolveActorPhotoUrl())
        val tempCommentId = "temp_comment_${System.currentTimeMillis()}"
        val optimisticComment = FeedComment(
            id = tempCommentId,
            postId = post.id,
            authorId = currentUserId,
            authorName = authorName,
            authorPhotoUrl = authorPhoto,
            text = cleanText,
            parentCommentId = parentCommentId.trim(),
            replyToAuthorName = replyToAuthorName.trim(),
            isEdited = false,
            visibility = "PUBLIC",
            createdAt = Timestamp.now(),
            updatedAt = Timestamp.now()
        )
        _localOptimisticComments.value = (_localOptimisticComments.value + optimisticComment).distinctBy { it.id }

        val prevOverride = _localPostEngagementOverrides.value[post.id] ?: PostEngagementOverride()
        _localPostEngagementOverrides.value = _localPostEngagementOverrides.value + (
            post.id to prevOverride.copy(commentCountDelta = prevOverride.commentCountDelta + 1)
            )

        viewModelScope.launch {
            val res = repository.addPostComment(
                post = post,
                authorName = authorName,
                authorPhotoUrl = authorPhoto,
                text = cleanText,
                parentCommentId = parentCommentId,
                replyToAuthorName = replyToAuthorName
            )
            res.onSuccess { realCommentId ->
                _localOptimisticComments.value = _localOptimisticComments.value
                    .filterNot { it.id == tempCommentId } +
                    optimisticComment.copy(id = realCommentId)
                val latestMap = _localPostEngagementOverrides.value.toMutableMap()
                val cur = latestMap[post.id]
                if (cur != null) {
                    val newDelta = (cur.commentCountDelta - 1).coerceAtLeast(0)
                    latestMap[post.id] = cur.copy(commentCountDelta = newDelta)
                    _localPostEngagementOverrides.value = latestMap
                }
                _feedbackBanner.value = if (parentCommentId.isNotBlank()) {
                    "আপনার উত্তর যুক্ত হয়েছে।"
                } else {
                    "আপনার কমেন্ট যুক্ত হয়েছে।"
                }
            }.onFailure { err ->
                _localOptimisticComments.value = _localOptimisticComments.value.filterNot { it.id == tempCommentId }
                val latestMap = _localPostEngagementOverrides.value.toMutableMap()
                val cur = latestMap[post.id]
                if (cur != null) {
                    val newDelta = (cur.commentCountDelta - 1).coerceAtLeast(0)
                    latestMap[post.id] = cur.copy(commentCountDelta = newDelta)
                    _localPostEngagementOverrides.value = latestMap
                }
                _errorBanner.value = err.localizedMessage ?: "কমেন্ট যুক্ত করা যায়নি।"
            }
        }
    }

    fun editPostComment(commentId: String, newText: String) {
        val cleanText = newText.trim()
        if (cleanText.isBlank()) {
            _errorBanner.value = "কমেন্ট খালি রাখা যাবে না।"
            return
        }
        val existingComment = ((postCommentsState.value as? UiState.Success)?.data ?: emptyList())
            .firstOrNull { it.id == commentId }
        if (existingComment != null) {
            val editedOptimistic = existingComment.copy(
                text = cleanText,
                isEdited = true,
                updatedAt = Timestamp.now()
            )
            _localOptimisticComments.value = _localOptimisticComments.value
                .filterNot { it.id == commentId } + editedOptimistic
        }
        viewModelScope.launch {
            val res = repository.editPostComment(commentId, cleanText)
            res.onSuccess {
                _feedbackBanner.value = "কমেন্ট সম্পাদনা করা হয়েছে।"
            }.onFailure { err ->
                _localOptimisticComments.value = _localOptimisticComments.value.filterNot { it.id == commentId }
                _errorBanner.value = err.localizedMessage ?: "কমেন্ট সম্পাদনা করা যায়নি।"
            }
        }
    }

    fun deletePostComment(commentId: String, postId: String, currentCommentCount: Int) {
        _locallyDeletedCommentIds.value = _locallyDeletedCommentIds.value + commentId
        _localOptimisticComments.value = _localOptimisticComments.value.filterNot { it.id == commentId }
        viewModelScope.launch {
            val res = repository.deletePostComment(commentId, postId, currentCommentCount)
            res.onSuccess {
                _feedbackBanner.value = "কমেন্ট মুছে ফেলা হয়েছে।"
            }.onFailure { err ->
                _locallyDeletedCommentIds.value = _locallyDeletedCommentIds.value - commentId
                _errorBanner.value = err.localizedMessage ?: "কমেন্ট মুছে ফেলা যায়নি।"
            }
        }
    }

    fun sharePostInApp(post: FeedPost, caption: String) {
        runSubmittingAction {
            val sharerName = resolveActorDisplayName("সম্মানিত দ্বীনি ভাই")
            val sharerPhoto = ProfilePhotoHelper.compactSenderPhotoForMessage(resolveActorPhotoUrl())
            val sharerDistrict = (userProfileState.value as? UiState.Success)?.data?.district.orEmpty()
            val targetOriginalId = post.sharedPostId.ifBlank { post.id }
            val targetOriginalAuthorId = post.sharedAuthorId.ifBlank { post.authorId }
            val targetOriginalAuthorName = post.sharedAuthorName.ifBlank { post.authorName.ifBlank { post.title } }
            val targetOriginalTitle = post.sharedTitle.ifBlank { post.title }
            val targetOriginalDesc = post.sharedDescription.ifBlank {
                buildString {
                    if (post.designation.isNotBlank()) append("পদ/বিভাগ: ${post.designation} | ")
                    if (post.district.isNotBlank()) append("জেলা: ${post.district} | ")
                    append(post.description)
                }
            }
            val targetOriginalType = post.sharedPostType.ifBlank { post.postType }

            val res = repository.shareFeedPostInApp(
                originalPost = post,
                sharerName = sharerName,
                sharerPhotoUrl = sharerPhoto,
                sharerDistrict = sharerDistrict,
                shareCaption = caption
            )
            res.onSuccess { newSharedPostId ->
                if (newSharedPostId.isNotBlank()) {
                    val optimisticSharedPost = FeedPost(
                        id = newSharedPostId,
                        authorId = currentUserId,
                        authorName = sharerName,
                        authorPhotoUrl = sharerPhoto,
                        postType = "GENERAL_POST",
                        title = caption.trim().ifBlank { "$targetOriginalAuthorName-এর পোস্ট শেয়ার করেছেন" }.take(200),
                        district = sharerDistrict.ifBlank { post.district },
                        upazilaOrLocation = post.upazilaOrLocation,
                        designation = post.designation.ifBlank { "দ্বীনি খেদমত শেয়ার" },
                        qualification = post.qualification,
                        salaryOrExperience = post.salaryOrExperience,
                        deadlineOrPreferredDistricts = post.deadlineOrPreferredDistricts,
                        description = caption.trim(),
                        contactNumber = post.contactNumber,
                        khedmotCategories = post.khedmotCategories,
                        customCategory = post.customCategory,
                        sharedPostId = targetOriginalId,
                        sharedAuthorId = targetOriginalAuthorId,
                        sharedAuthorName = targetOriginalAuthorName,
                        sharedTitle = targetOriginalTitle,
                        sharedDescription = targetOriginalDesc,
                        sharedPostType = targetOriginalType,
                        visibility = "PUBLIC",
                        createdAt = Timestamp.now(),
                        updatedAt = Timestamp.now()
                    )
                    _localOptimisticPosts.value = (listOf(optimisticSharedPost) + _localOptimisticPosts.value).distinctBy { it.id }
                }
                val prevOverride = _localPostEngagementOverrides.value[post.id] ?: PostEngagementOverride()
                val newExpectedShares = maxOf(post.shareCount + 1, prevOverride.minExpectedShareCount + 1)
                _localPostEngagementOverrides.value = _localPostEngagementOverrides.value + (
                    post.id to prevOverride.copy(minExpectedShareCount = newExpectedShares)
                    )
                _feedbackBanner.value = "আলহামদুলিল্লাহ, পোস্টটি আপনার পরিচয়ে ফিডে শেয়ার করা হয়েছে।"
            }.onFailure { err ->
                _errorBanner.value = err.localizedMessage ?: "পোস্ট শেয়ার করা যায়নি।"
            }
        }
    }

    fun reportPost(post: FeedPost, reason: String) {
        viewModelScope.launch {
            val reporterName = resolveActorDisplayName("ব্যবহারকারী")
            val res = repository.reportFeedPost(post, reporterName, reason)
            res.onSuccess {
                _feedbackBanner.value = "জাযাকাল্লাহু খাইরান। পোস্টটি পর্যালোচনার জন্য অ্যাডমিন প্যানেলে পাঠানো হয়েছে।"
            }.onFailure { err ->
                _errorBanner.value = err.localizedMessage ?: "রিপোর্ট পাঠানো যায়নি।"
            }
        }
    }

    fun deletePost(postId: String) {
        viewModelScope.launch {
            clearBanners()
            _localOptimisticPosts.value = _localOptimisticPosts.value.filterNot { it.id == postId }
            val result = repository.deleteFeedPost(postId)
            result.onSuccess {
                _feedbackBanner.value = "পোস্টটি মুছে ফেলা হয়েছে।"
            }.onFailure { err ->
                _errorBanner.value = err.localizedMessage ?: "মুছে ফেলতে সমস্যা হয়েছে।"
            }
        }
    }

    fun toggleBookmark(post: FeedPost, isCurrentlyBookmarked: Boolean) {
        viewModelScope.launch {
            clearBanners()
            val result = repository.toggleBookmark(post, isCurrentlyBookmarked)
            result.onSuccess { added ->
                _feedbackBanner.value = if (added) {
                    "পছন্দের খেদমত তালিকায় সংরক্ষিত হয়েছে।"
                } else {
                    "পছন্দের খেদমত তালিকা থেকে সরানো হয়েছে।"
                }
            }.onFailure { err ->
                _errorBanner.value = err.localizedMessage ?: "সংরক্ষণ করা যায়নি।"
            }
        }
    }

    fun registerMadrasa(
        name: String,
        district: String,
        address: String,
        establishedYear: String,
        highestMarhala: String,
        facilities: String,
        studentCount: String,
        contactPhone: String,
        onSuccessNavigate: () -> Unit = {}
    ) {
        if (name.isBlank() || district.isBlank()) {
            _errorBanner.value = "অনুগ্রহ করে মাদরাসার নাম ও জেলা উল্লেখ করুন।"
            return
        }
        runSubmittingAction {
            val result = repository.createMadrasa(
                name = name,
                district = district,
                address = address,
                establishedYear = establishedYear,
                highestMarhala = highestMarhala,
                facilities = facilities,
                studentCount = studentCount,
                contactPhone = contactPhone
            )
            result.onSuccess {
                _feedbackBanner.value = "আলহামদুলিল্লাহ, মাদরাসার তথ্য ডিরেক্টরিতে যুক্ত হয়েছে।"
                onSuccessNavigate()
            }.onFailure { err ->
                _errorBanner.value = err.localizedMessage ?: "মাদরাসা যুক্ত করতে ত্রুটি হয়েছে।"
            }
        }
    }

    fun submitKhedmotApplication(
        post: FeedPost,
        applicantName: String,
        applicantQualification: String,
        applicantExperience: String,
        applicantPhone: String,
        coverNote: String,
        onComplete: () -> Unit = {}
    ) {
        if (applicantName.isBlank() || applicantPhone.isBlank()) {
            _errorBanner.value = "অনুগ্রহ করে আপনার নাম ও যোগাযোগের নম্বর উল্লেখ করুন।"
            return
        }
        runSubmittingAction {
            val result = repository.submitApplicationToCircular(
                post = post,
                applicantName = applicantName,
                applicantQualification = applicantQualification,
                applicantExperience = applicantExperience,
                applicantPhone = applicantPhone,
                coverNote = coverNote
            )
            result.onSuccess { submissionId ->
                if (submissionId.isNotBlank()) {
                    val optimisticSub = KhedmotSubmission(
                        id = submissionId,
                        postId = post.id,
                        postTitle = post.title,
                        postDesignation = post.designation,
                        applicantId = currentUserId,
                        applicantName = applicantName.trim(),
                        applicantQualification = applicantQualification.trim(),
                        applicantExperience = applicantExperience.trim(),
                        applicantPhone = applicantPhone.trim(),
                        coverNote = coverNote.trim(),
                        recipientId = post.authorId,
                        status = "PENDING",
                        createdAt = Timestamp.now(),
                        updatedAt = Timestamp.now()
                    )
                    _localOptimisticSubmissions.value = (listOf(optimisticSub) + _localOptimisticSubmissions.value).distinctBy { it.id }
                }
                _feedbackBanner.value = "আলহামদুলিল্লাহ, আপনার খেদমতের আবেদন সংশ্লিষ্ট মাদরাসায় প্রেরিত হয়েছে।"
                _selectedPostForApply.value = null
                onComplete()
            }.onFailure { err ->
                _errorBanner.value = err.localizedMessage ?: "আবেদন প্রেরণে ত্রুটি হয়েছে।"
            }
        }
    }

    fun updateReceivedSubmissionStatus(submission: KhedmotSubmission, newStatus: String) {
        viewModelScope.launch {
            clearBanners()
            val actorName = resolveActorDisplayName("মাদরাসা কর্তৃপক্ষ")
            val result = repository.updateSubmissionStatus(submission, newStatus, actorName)
            result.onSuccess {
                _feedbackBanner.value = "আবেদনের অবস্থা হালনাগাদ করা হয়েছে এবং প্রার্থীকে নোটিফিকেশন পাঠানো হয়েছে।"
            }.onFailure { err ->
                _errorBanner.value = err.localizedMessage ?: "অবস্থা পরিবর্তনে ত্রুটি হয়েছে।"
            }
        }
    }

    fun sendMessage(
        receiverId: String,
        receiverName: String,
        subject: String,
        body: String,
        imageUrls: List<String> = emptyList(),
        groupId: String = "",
        groupName: String = "",
        groupMemberIds: List<String> = emptyList(),
        existingMessageId: String = "",
        onSent: () -> Unit = {}
    ) {
        val isGroupMsg = groupId.isNotBlank() || receiverId.startsWith("GROUP:")
        if (!isGroupMsg && (receiverId.isBlank() || receiverId == currentUserId)) {
            _errorBanner.value = "সঠিক প্রাপক নির্বাচন করুন।"
            return
        }
        val cleanBody = body.trim()
        val cappedImages = imageUrls.map { it.trim() }.filter { it.isNotBlank() }.take(5)
        if (cleanBody.isBlank() && cappedImages.isEmpty()) {
            _errorBanner.value = "অনুগ্রহ করে পয়গামের কথা লিখুন অথবা ছবি যুক্ত করুন।"
            return
        }
        if (imageUrls.size > 5) {
            _errorBanner.value = "একবারে সর্বোচ্চ ৫টি ছবি পাঠানো যাবে।"
            return
        }

        val senderName = resolveActorDisplayName("দ্বীনি ভাই")
        val senderPhoto = resolveActorPhotoUrl()
        val resolvedGroupId = groupId.ifBlank {
            if (receiverId.startsWith("GROUP:")) receiverId.removePrefix("GROUP:") else ""
        }
        val messageId = existingMessageId.trim().ifBlank {
            runCatching { repository.generateMessageDocumentId() }.getOrDefault("").ifBlank {
                "msg_${currentUserId.take(6)}_${System.currentTimeMillis()}"
            }
        }

        // Prevent duplicate concurrent in-flight dispatches of the exact same messageId
        if (!inFlightMessageIds.add(messageId)) {
            onSent()
            return
        }

        // Preserve original timestamp when retrying a message so conversation order never jumps
        val existingCreatedAt = _localOptimisticMessages.value
            .firstOrNull { it.id == messageId }
            ?.createdAt
            ?: Timestamp.now()

        val resolvedSubject = subject.ifBlank { "খেদমত সংক্রান্ত যোগাযোগ" }
        val resolvedGroupName = groupName.ifBlank { receiverName }

        val optimisticMsg = DirectMessage(
            id = messageId,
            senderId = currentUserId,
            senderName = senderName,
            senderPhotoUrl = senderPhoto,
            receiverId = if (isGroupMsg) "GROUP:$resolvedGroupId" else receiverId,
            receiverName = receiverName,
            groupId = resolvedGroupId,
            groupName = resolvedGroupName,
            groupMemberIds = if (isGroupMsg) (groupMemberIds + currentUserId).distinct() else emptyList(),
            subject = resolvedSubject,
            body = cleanBody,
            imageUrls = cappedImages,
            isRead = false,
            readByUserIds = listOf(currentUserId),
            createdAt = existingCreatedAt
        ).apply {
            localDeliveryState = "SENDING"
        }

        // Insert or update in-place by messageId so retrying NEVER duplicates the message
        _localOptimisticMessages.value = (
            listOf(optimisticMsg) + _localOptimisticMessages.value.filterNot { it.id == messageId }
        ).distinctBy { it.id }

        val queueDraft = FailedMessageDraft(
            partnerId = receiverId,
            body = cleanBody,
            imageUrls = cappedImages,
            messageId = messageId,
            receiverName = receiverName,
            subject = resolvedSubject,
            groupId = resolvedGroupId,
            groupName = resolvedGroupName,
            groupMemberIds = groupMemberIds,
            timestampMs = existingCreatedAt.toDate().time
        )
        pendingOutgoingQueue[messageId] = queueDraft

        // Clear composer draft immediately so the user can continue typing without UI freeze
        onSent()

        viewModelScope.launch {
            _errorBanner.value = null
            try {
                val sendResult = try {
                    withTimeout(10_000L) {
                        repository.sendDirectMessage(
                            senderName = senderName,
                            receiverId = receiverId,
                            receiverName = receiverName,
                            subject = resolvedSubject,
                            body = cleanBody,
                            senderPhotoUrl = senderPhoto,
                            imageUrls = cappedImages,
                            groupId = resolvedGroupId,
                            groupName = resolvedGroupName,
                            groupMemberIds = groupMemberIds,
                            preallocatedMessageId = messageId
                        )
                    }
                } catch (timeoutEx: TimeoutCancellationException) {
                    Result.failure(timeoutEx)
                }

                if (sendResult.isSuccess) {
                    val finalId = sendResult.getOrNull()?.ifBlank { messageId } ?: messageId
                    pendingOutgoingQueue.remove(messageId)
                    pendingOutgoingQueue.remove(finalId)
                    if (_failedMessageDraft.value?.messageId == finalId || _failedMessageDraft.value?.partnerId == receiverId) {
                        _failedMessageDraft.value = null
                    }
                    val sentMsg = optimisticMsg.copy(id = finalId).apply {
                        localDeliveryState = "SENT"
                    }
                    _localOptimisticMessages.value = (
                        listOf(sentMsg) + _localOptimisticMessages.value.filterNot { it.id == messageId || it.id == finalId }
                    ).distinctBy { it.id }
                } else {
                    val lastFailure = sendResult.exceptionOrNull()
                    pendingOutgoingQueue.remove(messageId)
                    val failedMsg = optimisticMsg.copy().apply {
                        localDeliveryState = "FAILED"
                    }
                    _localOptimisticMessages.value = (
                        listOf(failedMsg) + _localOptimisticMessages.value.filterNot { it.id == messageId }
                    ).distinctBy { it.id }
                    _failedMessageDraft.value = queueDraft
                    _errorBanner.value = if (lastFailure is TimeoutCancellationException) {
                        "সার্ভারে মেসেজ সংরক্ষণে নির্ধারিত সময়ের বেশি লাগছে। 'পুনরায় পাঠান' বাটনে চাপ দিন।"
                    } else {
                        lastFailure?.localizedMessage ?: "পয়গাম পাঠানো যায়নি। পুনরায় চেষ্টা করুন।"
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                pendingOutgoingQueue.remove(messageId)
                val failedMsg = optimisticMsg.copy().apply {
                    localDeliveryState = "FAILED"
                }
                _localOptimisticMessages.value = (
                    listOf(failedMsg) + _localOptimisticMessages.value.filterNot { it.id == messageId }
                ).distinctBy { it.id }
                _failedMessageDraft.value = queueDraft
                _errorBanner.value = e.localizedMessage ?: "পয়গাম পাঠানো যায়নি। পুনরায় চেষ্টা করুন।"
            } finally {
                inFlightMessageIds.remove(messageId)
            }
        }
    }

    /**
     * Retries a failed message in-place using its existing document ID so duplicate messages are never created.
     */
    fun retryFailedMessage(message: DirectMessage) {
        if (message.id.isBlank()) return
        val cleanBody = message.cleanDisplayBody()
        sendMessage(
            receiverId = message.receiverId,
            receiverName = message.receiverName,
            subject = message.subject.ifBlank { "খেদমত সংক্রান্ত যোগাযোগ" },
            body = cleanBody,
            imageUrls = message.imageUrls,
            groupId = message.groupId,
            groupName = message.groupName,
            groupMemberIds = message.groupMemberIds,
            existingMessageId = message.id
        )
    }

    fun createChatGroup(
        name: String,
        photoUrl: String,
        initialMemberIds: List<String>,
        initialMemberNames: Map<String, String>,
        onCreated: (ChatGroup) -> Unit = {}
    ) {
        val trimmedName = name.trim()
        if (trimmedName.isBlank()) {
            _errorBanner.value = "অনুগ্রহ করে গ্রুপের নাম লিখুন।"
            return
        }
        runSubmittingAction {
            val creatorName = resolveActorDisplayName("গ্রুপ অ্যাডমিন")
            val result = repository.createChatGroup(
                name = trimmedName,
                photoUrl = photoUrl,
                creatorName = creatorName,
                memberIds = initialMemberIds,
                memberNames = initialMemberNames
            )
            result.onSuccess { groupId ->
                val uid = currentUserId
                val finalMembers = (initialMemberIds + uid).map { it.trim() }.filter { it.isNotBlank() }.distinct()
                val createdGroup = ChatGroup(
                    id = groupId,
                    name = trimmedName,
                    photoUrl = photoUrl,
                    creatorId = uid,
                    creatorName = creatorName,
                    memberIds = finalMembers,
                    memberNames = initialMemberNames + (uid to creatorName)
                )
                _feedbackBanner.value = "আলহামদুলিল্লাহ, '${createdGroup.name}' গ্রুপ তৈরি হয়েছে।"
                onCreated(createdGroup)
            }.onFailure { err ->
                _errorBanner.value = err.localizedMessage ?: "গ্রুপ তৈরি করতে ত্রুটি হয়েছে।"
            }
        }
    }

    fun updateChatGroupDetails(
        groupId: String,
        newName: String,
        newPhotoUrl: String
    ) {
        if (groupId.isBlank() || newName.trim().isBlank()) return
        viewModelScope.launch {
            clearBanners()
            val result = repository.updateChatGroupDetails(
                groupId = groupId,
                name = newName.trim(),
                photoUrl = newPhotoUrl
            )
            result.onSuccess {
                _feedbackBanner.value = "আলহামদুলিল্লাহ, গ্রুপের নাম ও ছবি হালনাগাদ হয়েছে।"
            }.onFailure { err ->
                _errorBanner.value = err.localizedMessage ?: "গ্রুপ হালনাগাদ করতে ত্রুটি হয়েছে।"
            }
        }
    }

    fun addMemberToChatGroup(
        groupId: String,
        newMemberId: String,
        newMemberName: String
    ) {
        if (groupId.isBlank() || newMemberId.isBlank()) return
        val currentGroup = (userGroupsState.value as? UiState.Success)?.data?.firstOrNull { it.id == groupId }
            ?: return
        viewModelScope.launch {
            clearBanners()
            val result = repository.addMemberToChatGroup(
                group = currentGroup,
                newMemberId = newMemberId.trim(),
                newMemberName = newMemberName.trim().ifBlank { "সম্মানিত সদস্য" }
            )
            result.onSuccess {
                _feedbackBanner.value = "আলহামদুলিল্লাহ, নতুন সদস্য গ্রুপে যুক্ত করা হয়েছে।"
            }.onFailure { err ->
                _errorBanner.value = err.localizedMessage ?: "সদস্য যুক্ত করতে ত্রুটি হয়েছে।"
            }
        }
    }

    fun removeMemberFromChatGroup(
        groupId: String,
        memberIdToRemove: String
    ) {
        if (groupId.isBlank() || memberIdToRemove.isBlank()) return
        val currentGroup = (userGroupsState.value as? UiState.Success)?.data?.firstOrNull { it.id == groupId }
            ?: return
        viewModelScope.launch {
            clearBanners()
            val result = repository.removeMemberFromChatGroup(
                group = currentGroup,
                targetMemberId = memberIdToRemove
            )
            result.onSuccess {
                _feedbackBanner.value = "সদস্যকে গ্রুপ থেকে বাদ দেওয়া হয়েছে।"
            }.onFailure { err ->
                _errorBanner.value = err.localizedMessage ?: "সদস্য বাদ দিতে ত্রুটি হয়েছে।"
            }
        }
    }

    fun markMessageRead(messageId: String) {
        if (messageId.isBlank()) return
        _locallyReadMessageIds.value = _locallyReadMessageIds.value + messageId
        viewModelScope.launch {
            repository.markMessageAsRead(messageId)
        }
    }

    fun markConversationRead(otherUserId: String) {
        if (otherUserId.isBlank() || otherUserId == currentUserId) return
        val currentMessages = (messagesState.value as? UiState.Success)?.data ?: emptyList()
        val isGroupKey = otherUserId.startsWith("GROUP:", ignoreCase = true)
        val groupId = if (isGroupKey) otherUserId.substringAfter(":") else ""
        val matchingUnreadMessages = currentMessages.filter { msg ->
            msg.id.isNotBlank() && msg.isUnreadForUser(currentUserId) && (
                if (isGroupKey) {
                    msg.groupId == groupId || msg.receiverId.equals("GROUP:$groupId", ignoreCase = true)
                } else {
                    msg.groupId.isBlank() && msg.senderId == otherUserId && msg.receiverId == currentUserId
                }
            )
        }
        val matchingUnreadIds = matchingUnreadMessages.map { it.id }.toSet()
        if (matchingUnreadIds.isNotEmpty()) {
            _locallyReadMessageIds.value = _locallyReadMessageIds.value + matchingUnreadIds
            viewModelScope.launch {
                repository.markConversationMessagesAsRead(otherUserId, matchingUnreadMessages)
            }
        } else if (currentMessages.isEmpty()) {
            viewModelScope.launch {
                repository.markConversationMessagesAsRead(otherUserId, emptyList())
            }
        }
    }

    fun reactToMessage(message: DirectMessage, reactionCode: String) {
        viewModelScope.launch {
            val result = repository.reactToMessage(message, reactionCode)
            result.onFailure { err ->
                _errorBanner.value = err.localizedMessage ?: "অভিবাদন যুক্ত করতে ত্রুটি হয়েছে।"
            }
        }
    }

    fun editMessage(message: DirectMessage, newBody: String) {
        val trimmed = newBody.trim()
        if (trimmed.isBlank()) {
            _errorBanner.value = "অনুগ্রহ করে পয়গামের কথা লিখুন।"
            return
        }
        viewModelScope.launch {
            val result = repository.editDirectMessage(message, trimmed)
            result.onSuccess {
                _feedbackBanner.value = "আলহামদুলিল্লাহ, আপনার পয়গামটি সম্পাদিত হয়েছে।"
            }.onFailure { err ->
                _errorBanner.value = err.localizedMessage ?: "পয়গাম সম্পাদনা করতে ত্রুটি হয়েছে।"
            }
        }
    }

    fun deleteMessageForMe(message: DirectMessage) {
        viewModelScope.launch {
            val result = repository.deleteMessageForMe(message)
            result.onSuccess {
                _feedbackBanner.value = "পয়গামটি আপনার কথোপকথন থেকে সরিয়ে দেওয়া হয়েছে।"
            }.onFailure { err ->
                _errorBanner.value = err.localizedMessage ?: "পয়গাম মুছতে ত্রুটি হয়েছে।"
            }
        }
    }

    fun deleteMessageForEveryone(message: DirectMessage) {
        viewModelScope.launch {
            val result = repository.deleteMessageForEveryone(message)
            result.onSuccess {
                _feedbackBanner.value = "পয়গামটি উভয়ের কথোপকথন থেকে প্রত্যাহার করা হয়েছে।"
            }.onFailure { err ->
                _errorBanner.value = err.localizedMessage ?: "পয়গাম প্রত্যাহার করতে ত্রুটি হয়েছে।"
            }
        }
    }

    fun toggleBlockUser(targetUserId: String, block: Boolean) {
        if (targetUserId.isBlank() || targetUserId == currentUserId) return
        viewModelScope.launch {
            val defaultName = resolveActorDisplayName("সম্মানিত সদস্য")
            val result = repository.setBlockedUserStatus(targetUserId, block, defaultName)
            result.onSuccess {
                _feedbackBanner.value = if (block) {
                    "এই ব্যবহারকারীর সাথে পয়গাম আদান-প্রদান বিরত (ব্লক) রাখা হয়েছে।"
                } else {
                    "আলহামদুলিল্লাহ, এই ব্যবহারকারীর সাথে পয়গাম আদান-প্রদান পুনরায় চালু হয়েছে।"
                }
            }.onFailure { err ->
                _errorBanner.value = err.localizedMessage ?: "ব্লক স্ট্যাটাস পরিবর্তন করতে ত্রুটি হয়েছে।"
            }
        }
    }

    fun saveConversationTheme(partnerUserId: String, themeSpec: String) {
        if (partnerUserId.isBlank()) return
        viewModelScope.launch {
            val defaultName = resolveActorDisplayName("সম্মানিত সদস্য")
            val result = repository.updateConversationTheme(partnerUserId, themeSpec, defaultName)
            result.onSuccess {
                _feedbackBanner.value = "আলহামদুলিল্লাহ, কথোপকথনের আবহ ও থিম সংরক্ষিত হয়েছে।"
            }.onFailure { err ->
                _errorBanner.value = err.localizedMessage ?: "থিম সংরক্ষণ করতে ত্রুটি হয়েছে।"
            }
        }
    }

    fun markNotificationRead(notification: AppNotification) {
        if (notification.isRead) return
        viewModelScope.launch {
            repository.markNotificationAsRead(notification)
        }
    }

    fun markAllNotificationsRead() {
        val currentList = (notificationsState.value as? UiState.Success)?.data ?: return
        val unreadList = currentList.filter { !it.isRead }
        if (unreadList.isEmpty()) return
        viewModelScope.launch {
            val result = repository.markAllNotificationsAsRead(unreadList)
            result.onSuccess {
                _feedbackBanner.value = "সকল নোটিফিকেশন পঠিত হিসেবে চিহ্নিত করা হয়েছে।"
            }
        }
    }

    fun openNotificationTarget(notification: AppNotification) {
        markNotificationRead(notification)
        when (notification.type) {
            "MESSAGE" -> {
                if (notification.senderId.isNotBlank() && notification.senderId != currentUserId) {
                    openDirectMessageTo(notification.senderId, notification.senderName)
                } else {
                    navigateTo(MainDestination.MESSAGES)
                }
            }
            "APPLICATION" -> navigateTo(MainDestination.ABEDON)
            "VERIFICATION" -> navigateTo(MainDestination.VERIFIED_MADRASAS)
            "NEW_CIRCULAR", "BOOST" -> {
                val matchingPost = (feedPostsState.value as? UiState.Success)?.data
                    ?.find { it.id == notification.referenceId }
                openFeedWithFilter(FeedFilterTab.JOB_CIRCULAR)
                if (matchingPost != null) {
                    openPostDetailDialog(matchingPost)
                }
            }
            "NEW_APPLICATION" -> {
                val matchingPost = (feedPostsState.value as? UiState.Success)?.data
                    ?.find { it.id == notification.referenceId }
                openFeedWithFilter(FeedFilterTab.APPLICATIONS)
                if (matchingPost != null) {
                    openPostDetailDialog(matchingPost)
                }
            }
            "ANNOUNCEMENT" -> {
                if (notification.referenceId.startsWith("ajker_smoron", ignoreCase = true) ||
                    notification.title.contains("আজকের স্মরণ")
                ) {
                    val remId = notification.referenceId.removePrefix("ajker_smoron:")
                    openAjkerSmoronFromNotification(remId)
                }
            }
        }
    }

    fun openAjkerSmoronFromNotification(reminderId: String = "") {
        if (reminderId.isNotBlank()) {
            _focusedSmoronId.value = reminderId.trim()
        }
        navigateTo(MainDestination.AJKER_SMORON)
    }

    fun clearFocusedSmoronId() {
        _focusedSmoronId.value = null
    }

    fun syncSmoronAssignmentToServer(dateKey: String, reminderId: String) {
        viewModelScope.launch {
            repository.recordSmoronAssignmentInServer(dateKey, reminderId)
        }
    }

    fun syncSmoronPushSentToServer(dateKey: String, reminderId: String) {
        viewModelScope.launch {
            repository.recordSmoronPushSentInServer(dateKey, reminderId)
        }
    }

    fun syncSmoronAmolCompletedToServer(dateKey: String) {
        viewModelScope.launch {
            repository.recordSmoronAmolCompletedInServer(dateKey)
        }
    }

    // --- ADMIN PANEL & SECURITY ACTIONS (Strictly gated by verified Admin Identity) ---

    fun adminVerifyMadrasa(madrasa: MadrasaProfile, verified: Boolean) {
        if (!isAdminUser) {
            _errorBanner.value = "শুধুমাত্র অনুমোদিত অ্যাডমিন এই কার্যক্রম পরিচালনা করতে পারবেন।"
            return
        }
        viewModelScope.launch {
            clearBanners()
            val adminName = resolveActorDisplayName("নাজিমে আলা (অ্যাডমিন)")
            val result = repository.adminToggleMadrasaVerification(madrasa, verified, adminName)
            result.onSuccess {
                _feedbackBanner.value = if (verified) {
                    "মাদরাসাটি 'যাচাইকৃত মাদরাসা' হিসেবে অনুমোদিত হয়েছে এবং টেলিগ্রাম ইত্তেলা লগ করা হয়েছে।"
                } else {
                    "মাদরাসার যাচাইকরণ স্ট্যাটাস পরিবর্তন করা হয়েছে।"
                }
            }.onFailure { err ->
                _errorBanner.value = err.localizedMessage ?: "অ্যাডমিন অনুমোদন সম্পন্ন হয়নি।"
            }
        }
    }

    fun adminVerifyPost(post: FeedPost, isVerified: Boolean, approvalStatus: String) {
        if (!isAdminUser) {
            _errorBanner.value = "শুধুমাত্র অনুমোদিত অ্যাডমিন এই কার্যক্রম পরিচালনা করতে পারবেন।"
            return
        }
        viewModelScope.launch {
            clearBanners()
            val adminName = resolveActorDisplayName("নাজিমে আলা (অ্যাডমিন)")
            val result = repository.adminUpdatePostVerification(post, isVerified, approvalStatus, adminName)
            result.onSuccess {
                _feedbackBanner.value = "পোস্টের অনুমোদন অবস্থা হালনাগাদ হয়েছে।"
            }.onFailure { err ->
                _errorBanner.value = err.localizedMessage ?: "পোস্ট হালনাগাদ ব্যর্থ হয়েছে।"
            }
        }
    }

    fun adminToggleBoostPost(post: FeedPost, isBoosted: Boolean) {
        if (!isAdminUser) {
            _errorBanner.value = "শুধুমাত্র অনুমোদিত অ্যাডমিন বিজ্ঞপ্তি Boost করতে পারবেন।"
            return
        }
        viewModelScope.launch {
            clearBanners()
            val adminName = resolveActorDisplayName("নাজিমে আলা (অ্যাডমিন)")
            val result = repository.adminTogglePostBoost(post, isBoosted, adminName)
            result.onSuccess {
                _feedbackBanner.value = if (isBoosted) {
                    "আলহামদুলিল্লাহ, বিজ্ঞপ্তিটি Boosted হিসেবে ফিডের উপরে স্থাপন করা হয়েছে।"
                } else {
                    "বিজ্ঞপ্তিটির Boosted অবস্থা সরানো হয়েছে।"
                }
            }.onFailure { err ->
                _errorBanner.value = err.localizedMessage ?: "Boost অবস্থা পরিবর্তন ব্যর্থ হয়েছে।"
            }
        }
    }

    fun publishAdminAnnouncement(
        title: String,
        message: String,
        onSent: () -> Unit = {}
    ) {
        if (!isAdminUser) {
            _errorBanner.value = "শুধুমাত্র অনুমোদিত অ্যাডমিন ঘোষণা প্রকাশ করতে পারবেন।"
            return
        }
        if (title.isBlank() || message.isBlank()) {
            _errorBanner.value = "অনুগ্রহ করে ঘোষণার শিরোনাম ও বিস্তারিত বার্তা লিখুন।"
            return
        }
        runSubmittingAction {
            val adminName = resolveActorDisplayName("নাজিমে আলা (অ্যাডমিন)")
            val result = repository.sendAdminAnnouncement(adminName, title, message)
            result.onSuccess {
                _feedbackBanner.value = "আলহামদুলিল্লাহ, অ্যাডমিন ঘোষণা সকল ব্যবহারকারীর কাছে ও টেলিগ্রাম লগে প্রেরিত হয়েছে।"
                onSent()
            }.onFailure { err ->
                _errorBanner.value = err.localizedMessage ?: "ঘোষণা প্রেরণ করা যায়নি।"
            }
        }
    }

    fun requestAdminPasswordReset(email: String) {
        runSubmittingAction {
            val result = repository.sendAdminPasswordResetLink(email)
            result.onSuccess { sentEmail ->
                _feedbackBanner.value = "আলহামদুলিল্লাহ, অনুমোদিত অ্যাডমিন ইমেইল ($sentEmail)-এ Firebase Authentication থেকে নিরাপদ Password Reset Link পাঠানো হয়েছে।"
            }.onFailure { err ->
                _errorBanner.value = err.localizedMessage ?: "পাসওয়ার্ড রিসেট লিংক পাঠানো যায়নি।"
            }
        }
    }

    fun sendTestTelegramNotification(customMessage: String = "") {
        if (!isAdminUser) {
            _errorBanner.value = "শুধুমাত্র অনুমোদিত অ্যাডমিন টেলিগ্রাম নোটিফিকেশন পরীক্ষা করতে পারবেন।"
            return
        }
        runSubmittingAction {
            val adminName = resolveActorDisplayName("নাজিমে আলা (অ্যাডমিন)")
            val result = repository.sendAdminTestTelegramNotification(adminName, customMessage)
            result.onSuccess { verifiedMessage ->
                _feedbackBanner.value = verifiedMessage
            }.onFailure { err ->
                _errorBanner.value = err.localizedMessage ?: "টেলিগ্রাম বটে মেসেজ পাঠানো যায়নি। বট টোকেন ও সংযোগ যাচাই করুন।"
            }
        }
    }

    fun showFeedbackMessage(message: String) {
        _errorBanner.value = null
        _feedbackBanner.value = message
    }

    fun adminUpdateUserVerification(user: UserProfile, verified: Boolean) {
        if (!isAdminUser) {
            _errorBanner.value = "শুধুমাত্র অনুমোদিত অ্যাডমিন এই কার্যক্রম পরিচালনা করতে পারবেন।"
            return
        }
        viewModelScope.launch {
            clearBanners()
            val adminName = resolveActorDisplayName("নাজিমে আলা (অ্যাডমিন)")
            val result = repository.adminUpdateUserVerification(user, verified, adminName)
            result.onSuccess {
                val tgConfigured = com.example.ui.util.TelegramNotificationHelper.isTelegramBotConfigured()
                val tgSuffix = if (tgConfigured) " এবং টেলিগ্রাম অডিট লগ হালনাগাদ হয়েছে।" else " (অ্যাডমিন লগে সংরক্ষিত হয়েছে)।"
                _feedbackBanner.value = if (verified) {
                    "ব্যবহারকারীকে সক্রিয় করা হয়েছে$tgSuffix"
                } else {
                    "ব্যবহারকারী ব্লক / নিষ্ক্রিয় করা হয়েছে$tgSuffix"
                }
            }.onFailure { err ->
                _errorBanner.value = err.localizedMessage ?: "ব্যবহারকারীর অবস্থা পরিবর্তন ব্যর্থ হয়েছে।"
            }
        }
    }

    fun adminUpdateSubmissionStatus(submission: KhedmotSubmission, newStatus: String) {
        if (!isAdminUser) {
            _errorBanner.value = "শুধুমাত্র অনুমোদিত অ্যাডমিন আবেদনের অবস্থা পরিবর্তন করতে পারবেন।"
            return
        }
        viewModelScope.launch {
            clearBanners()
            val adminName = resolveActorDisplayName("নাজিমে আলা (অ্যাডমিন)")
            val result = repository.adminUpdateSubmissionStatus(submission, newStatus, adminName)
            result.onSuccess {
                _feedbackBanner.value = "আবেদনের অবস্থা হালনাগাদ হয়েছে এবং অ্যাডমিন অডিট লগে সংরক্ষিত হয়েছে।"
            }.onFailure { err ->
                _errorBanner.value = err.localizedMessage ?: "আবেদনের অবস্থা পরিবর্তন ব্যর্থ হয়েছে।"
            }
        }
    }

    fun adminRecordChangeNotification(
        eventType: String,
        title: String,
        message: String,
        referenceId: String = "admin_action"
    ) {
        if (!isAdminUser) return
        viewModelScope.launch {
            clearBanners()
            val adminName = resolveActorDisplayName("নাজিমে আলা (অ্যাডমিন)")
            val result = repository.adminRecordLiveChangeNotification(
                eventType = eventType,
                title = title,
                message = message,
                adminName = adminName,
                referenceId = referenceId
            )
            result.onSuccess { dispatchSummary ->
                if (dispatchSummary.startsWith("FIRESTORE_SAVED_TELEGRAM_FAILED::")) {
                    val reason = dispatchSummary.removePrefix("FIRESTORE_SAVED_TELEGRAM_FAILED::")
                    _feedbackBanner.value = "$title — অ্যাডমিন লগে সংরক্ষিত হয়েছে (টেলিগ্রাম ডেলিভারি ব্যর্থ: $reason)"
                } else {
                    _feedbackBanner.value = "$title — টেলিগ্রাম বটে সফলভাবে পাঠানো হয়েছে।"
                }
            }.onFailure { err ->
                _errorBanner.value = err.localizedMessage ?: "টেলিগ্রাম বটে ইত্তেলা পাঠানো যায়নি।"
            }
        }
    }

    fun adminPublishQuickNotice(
        title: String,
        description: String,
        noticeType: String,
        onComplete: () -> Unit = {}
    ) {
        if (!isAdminUser) {
            _errorBanner.value = "শুধুমাত্র অনুমোদিত অ্যাডমিন বিজ্ঞপ্তি প্রকাশ করতে পারবেন।"
            return
        }
        if (title.isBlank() || description.isBlank()) {
            _errorBanner.value = "অনুগ্রহ করে বিজ্ঞপ্তির শিরোনাম ও বিবরণ পূরণ করুন।"
            return
        }
        runSubmittingAction(timeoutMs = 35_000L) {
            val adminName = resolveActorDisplayName("নাজিমে আলা (অ্যাডমিন)")
            val isFeatured = noticeType == "প্রধান" || noticeType == "জরুরি"
            val result = repository.createFeedPost(
                authorName = adminName,
                postType = "JOB_CIRCULAR",
                title = title.trim(),
                district = "সারা বাংলাদেশ",
                upazilaOrLocation = noticeType,
                designation = if (noticeType == "জরুরি") "জরুরি বিজ্ঞপ্তি" else "দ্বীনি খেদমত বিজ্ঞপ্তি",
                qualification = "সংশ্লিষ্ট মারহালা / বিভাগ",
                salaryOrExperience = "আলোচনা সাপেক্ষে",
                deadlineOrPreferredDistricts = noticeType,
                description = description.trim(),
                contactNumber = AdminSecurityHelper.ADMIN_MOBILE_NUMBER,
                authorPhotoUrl = resolveActorPhotoUrl()
            )
            result.onSuccess { postId ->
                val createdPost = FeedPost(
                    id = postId,
                    authorId = currentUserId,
                    authorName = adminName,
                    postType = "JOB_CIRCULAR",
                    title = title.trim(),
                    district = "সারা বাংলাদেশ",
                    upazilaOrLocation = noticeType,
                    designation = if (noticeType == "জরুরি") "জরুরি বিজ্ঞপ্তি" else "দ্বীনি খেদমত বিজ্ঞপ্তি",
                    qualification = "সংশ্লিষ্ট মারহালা / বিভাগ",
                    salaryOrExperience = "আলোচনা সাপেক্ষে",
                    deadlineOrPreferredDistricts = noticeType,
                    description = description.trim(),
                    contactNumber = AdminSecurityHelper.ADMIN_MOBILE_NUMBER,
                    isVerifiedMadrasa = isFeatured,
                    isBoosted = isFeatured,
                    visibility = "PUBLIC",
                    createdAt = Timestamp.now(),
                    updatedAt = Timestamp.now()
                )
                if (postId.isNotBlank()) {
                    _localOptimisticPosts.value = (listOf(createdPost) + _localOptimisticPosts.value).distinctBy { it.id }
                }
                if (isFeatured) {
                    repository.adminTogglePostBoost(createdPost, true, adminName)
                }
                val tgRes = repository.adminRecordLiveChangeNotification(
                    eventType = "ADMIN_ANNOUNCEMENT",
                    title = "বিজ্ঞপ্তি প্রকাশ করা হয়েছে ($noticeType)",
                    message = "শিরোনাম: ${title.trim()} | ধরন: $noticeType | বিবরণ: ${description.trim().take(200)}",
                    adminName = adminName,
                    referenceId = postId.ifBlank { "admin_notice" }
                )
                val tgSuccess = tgRes.getOrNull()?.startsWith("FIRESTORE_SAVED_TELEGRAM_FAILED::") == false
                _feedbackBanner.value = if (tgSuccess) {
                    "আলহামদুলিল্লাহ, নতুন বিজ্ঞপ্তি প্রকাশিত হয়েছে এবং টেলিগ্রাম বটে সফলভাবে পাঠানো হয়েছে।"
                } else {
                    "আলহামদুলিল্লাহ, নতুন বিজ্ঞপ্তি প্রকাশিত ও অ্যাডমিন লগে সংরক্ষিত হয়েছে (টেলিগ্রাম ডেলিভারি স্ট্যাটাস: ব্যর্থ/টোকেন যাচাই প্রয়োজন)।"
                }
                onComplete()
            }.onFailure { err ->
                _errorBanner.value = err.localizedMessage ?: "বিজ্ঞপ্তি প্রকাশ করা যায়নি।"
            }
        }
    }

    fun retryAdminServerStreams() {
        clearBanners()
        _adminDataRefreshToken.value += 1
        verifyAndSyncAdminIdentity()
    }

    fun submitUserSupportMessage(
        category: String,
        subject: String,
        message: String,
        contactPhone: String,
        onSuccessReset: () -> Unit = {}
    ) {
        if (message.isBlank()) {
            _errorBanner.value = "অনুগ্রহ করে আপনার বার্তা, অভিযোগ, সমস্যা বা পরামর্শ বিস্তারিত লিখুন।"
            return
        }
        runSubmittingAction {
            val senderName = resolveActorDisplayName("ব্যবহারকারী")
            val result = repository.submitSupportOrComplaintMessage(
                category = category,
                subject = subject,
                message = message,
                contactPhone = contactPhone,
                senderName = senderName
            )
            result.onSuccess { dispatchSummary ->
                if (dispatchSummary.startsWith("FIRESTORE_SAVED_TELEGRAM_FAILED::")) {
                    val reason = dispatchSummary.removePrefix("FIRESTORE_SAVED_TELEGRAM_FAILED::")
                    _feedbackBanner.value = "আপনার বার্তা সার্ভারে ও অ্যাডমিন প্যানেলে সংরক্ষিত হয়েছে। (টেলিগ্রাম বট ডেলিভারি স্ট্যাটাস: ব্যর্থ — $reason)"
                } else {
                    _feedbackBanner.value = "আলহামদুলিল্লাহ, আপনার বার্তা সার্ভারে সংরক্ষিত হয়েছে, অ্যাডমিন প্যানেলে যুক্ত হয়েছে এবং টেলিগ্রাম বটে সফলভাবে পাঠানো হয়েছে।"
                }
                onSuccessReset()
            }.onFailure { err ->
                _errorBanner.value = err.localizedMessage ?: "বার্তা পাঠানো যায়নি। অনুগ্রহ করে ইন্টারনেট সংযোগ যাচাই করে পুনরায় চেষ্টা করুন।"
            }
        }
    }

    fun adminPublishAppRelease(
        versionCode: Int,
        versionName: String,
        apkDownloadUrl: String,
        releaseNotes: String,
        apkSha256: String = "",
        apkFileSizeBytes: Long = 0L,
        isMandatory: Boolean = false,
        releaseStatus: String = "PUBLISHED",
        onComplete: () -> Unit = {}
    ) {
        if (!isAdminUser) {
            _errorBanner.value = "শুধুমাত্র অনুমোদিত অ্যাডমিন নতুন অ্যাপ আপডেট রিলিজ তথ্য প্রকাশ করতে পারবেন।"
            return
        }
        runSubmittingAction {
            val adminName = resolveActorDisplayName("নাজিমে আলা (অ্যাডমিন)")
            val result = repository.publishAppReleaseInfo(
                versionCode = versionCode,
                versionName = versionName,
                apkDownloadUrl = apkDownloadUrl,
                releaseNotes = releaseNotes,
                apkSha256 = apkSha256,
                apkFileSizeBytes = apkFileSizeBytes,
                isMandatory = isMandatory,
                releaseStatus = releaseStatus,
                publishedBy = adminName
            )
            result.onSuccess {
                _dismissedUpdateVersionCode.value = null
                val statusLabel = when (releaseStatus.uppercase()) {
                    "PAUSED" -> "সাময়িক স্থগিত"
                    "DISABLED" -> "বন্ধ"
                    else -> "প্রকাশিত"
                }
                _feedbackBanner.value = "আলহামদুলিল্লাহ, অ্যাপ আপডেট সংস্করণ ($versionName / কোড $versionCode — অবস্থা: $statusLabel) সংরক্ষিত হয়েছে।"
                onComplete()
            }.onFailure { err ->
                _errorBanner.value = err.localizedMessage ?: "নতুন আপডেট তথ্য সংরক্ষণ করা যায়নি।"
            }
        }
    }

    suspend fun recordAdminLogoutBeforeSignOut() {
        if (!isAdminUser) return
        val adminName = currentUserEmail.ifBlank { resolveActorDisplayName("নাজিমে আলা (অ্যাডমিন)") }
        repository.recordAdminLogoutActivity(adminName)
    }

    private companion object {
        const val TAG = "QawmiViewModel"
    }
}

