-- =====================================================================
-- FINANCE TRACKER - FILE TẠO CƠ SỞ DỮ LIỆU (PostgreSQL 15+)
-- =====================================================================
-- Thiết kế theo BG HP TTTN 2 CNPM 2020, mục 4.2. Chi tiết: database-schema.docx
--
-- File gồm:
--   1. Bảng + khóa chính, khóa ngoại, UNIQUE, CHECK (ràng buộc dữ liệu)
--   2. Index (gồm UNIQUE không phân biệt hoa thường, UNIQUE có điều kiện)
--   3. Trigger (ràng buộc liên bảng, tự cập nhật updatedAt)
--   4. View tính giá trị dẫn xuất (số dư nhóm, chi tiêu bucket)
--   5. Dữ liệu mẫu (kịch bản Alice - Bob - Cara trong đặc tả)
--
-- Flyway tự khởi tạo schema trên PostgreSQL trống khi backend khởi động.
--
-- Ghi chú: tên bảng/cột viết camelCase không đặt trong dấu nháy nên PostgreSQL
-- lưu dưới dạng chữ thường (tblUser -> tbluser). Truy vấn vẫn viết tblUser bình thường.
-- =====================================================================

-- =====================================================================
-- 1. BẢNG
-- =====================================================================

-- ----------------------------- PHẦN CÁ NHÂN --------------------------

CREATE TABLE tblUser (
    id           VARCHAR(36)  NOT NULL,
    email        VARCHAR(255) NOT NULL,
    passwordHash VARCHAR(255) NOT NULL,
    displayName  VARCHAR(50)  NOT NULL,
    timezone     VARCHAR(64)  NOT NULL DEFAULT 'UTC',
    notifyBucketAlerts BOOLEAN NOT NULL DEFAULT TRUE,
    notifyGroupAlerts  BOOLEAN NOT NULL DEFAULT TRUE,
    createdAt    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pkUser PRIMARY KEY (id),
    CONSTRAINT ckUserEmail CHECK (email ~ '^[^@\s]+@[^@\s]+\.[^@\s]+$'),
    CONSTRAINT ckUserDisplayName CHECK (length(trim(displayName)) BETWEEN 1 AND 50)
);

CREATE TABLE tblDevice (
    id          VARCHAR(36) NOT NULL,
    platform    VARCHAR(20) NOT NULL,
    lastSyncAt  TIMESTAMP,
    signedOutAt TIMESTAMP,
    refreshTokenHash      VARCHAR(255),
    refreshTokenExpiresAt TIMESTAMP,
    tblUserid   VARCHAR(36) NOT NULL,
    CONSTRAINT pkDevice PRIMARY KEY (id),
    CONSTRAINT fkDeviceUser FOREIGN KEY (tblUserid) REFERENCES tblUser (id) ON DELETE CASCADE,
    CONSTRAINT ckDevicePlatform CHECK (platform IN ('ios', 'android', 'web'))
);

CREATE TABLE tblCategory (
    id        VARCHAR(36) NOT NULL,
    name      VARCHAR(50) NOT NULL,
    isSystem  BOOLEAN     NOT NULL DEFAULT FALSE,
    version   INT         NOT NULL DEFAULT 1,
    createdAt TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updatedAt TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    tblUserid VARCHAR(36) NOT NULL,
    CONSTRAINT pkCategory PRIMARY KEY (id),
    CONSTRAINT fkCategoryUser FOREIGN KEY (tblUserid) REFERENCES tblUser (id) ON DELETE CASCADE,
    CONSTRAINT ckCategoryName CHECK (length(trim(name)) BETWEEN 1 AND 50),
    CONSTRAINT ckCategoryVersion CHECK (version >= 1)
);

CREATE TABLE tblSpendingBucket (
    id            VARCHAR(36)   NOT NULL,
    name          VARCHAR(50)   NOT NULL,
    monthlyTarget DECIMAL(12,2) NOT NULL,
    version       INT           NOT NULL DEFAULT 1,
    createdAt     TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updatedAt     TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    tblUserid     VARCHAR(36)   NOT NULL,
    CONSTRAINT pkSpendingBucket PRIMARY KEY (id),
    CONSTRAINT fkSpendingBucketUser FOREIGN KEY (tblUserid) REFERENCES tblUser (id) ON DELETE CASCADE,
    CONSTRAINT ckSpendingBucketName CHECK (length(trim(name)) BETWEEN 1 AND 50),
    CONSTRAINT ckSpendingBucketTarget CHECK (monthlyTarget > 0),
    CONSTRAINT ckSpendingBucketVersion CHECK (version >= 1)
);

-- Quan hệ n-n KHÔNG có thuộc tính: chỉ gồm 2 khóa ngoại làm khóa chính
CREATE TABLE tblBucketCategory (
    tblSpendingBucketid VARCHAR(36) NOT NULL,
    tblCategoryid       VARCHAR(36) NOT NULL,
    CONSTRAINT pkBucketCategory PRIMARY KEY (tblSpendingBucketid, tblCategoryid),
    CONSTRAINT fkBucketCategoryBucket FOREIGN KEY (tblSpendingBucketid) REFERENCES tblSpendingBucket (id) ON DELETE CASCADE,
    CONSTRAINT fkBucketCategoryCategory FOREIGN KEY (tblCategoryid) REFERENCES tblCategory (id) ON DELETE CASCADE
);

CREATE TABLE tblBucketAlert (
    id                  VARCHAR(36) NOT NULL,
    month               VARCHAR(7)     NOT NULL,
    threshold           INT         NOT NULL,
    sentAt              TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    tblSpendingBucketid VARCHAR(36) NOT NULL,
    CONSTRAINT pkBucketAlert PRIMARY KEY (id),
    CONSTRAINT uqBucketAlert UNIQUE (tblSpendingBucketid, month, threshold),
    CONSTRAINT fkBucketAlertBucket FOREIGN KEY (tblSpendingBucketid) REFERENCES tblSpendingBucket (id) ON DELETE CASCADE,
    CONSTRAINT ckBucketAlertMonth CHECK (month ~ '^[0-9]{4}-(0[1-9]|1[0-2])$'),
    CONSTRAINT ckBucketAlertThreshold CHECK (threshold IN (80, 100))
);

-- ------------------------------- PHẦN NHÓM ---------------------------

CREATE TABLE tblGroup (
    id            VARCHAR(36)  NOT NULL,
    name          VARCHAR(100) NOT NULL,
    description   VARCHAR(500),
    status        VARCHAR(10)  NOT NULL DEFAULT 'active',
    createdAt     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    closedAt      TIMESTAMP,
    creatorUserid VARCHAR(36)  NOT NULL,
    CONSTRAINT pkGroup PRIMARY KEY (id),
    CONSTRAINT fkGroupCreator FOREIGN KEY (creatorUserid) REFERENCES tblUser (id),
    CONSTRAINT ckGroupName CHECK (length(trim(name)) BETWEEN 1 AND 100),
    CONSTRAINT ckGroupStatus CHECK (status IN ('active', 'closed')),
    CONSTRAINT ckGroupClosedAt CHECK ((status = 'closed') = (closedAt IS NOT NULL))
);

-- Quan hệ n-n CÓ thuộc tính (đã gộp MemberSnapshot 1-1)
CREATE TABLE tblGroupMember (
    id                 VARCHAR(36)  NOT NULL,
    role               VARCHAR(10)  NOT NULL DEFAULT 'member',
    status             VARCHAR(10)  NOT NULL DEFAULT 'active',
    joinedAt           TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    leftAt             TIMESTAMP,
    hiddenAt           TIMESTAMP,
    retainedGroupName  VARCHAR(100),
    snapshotCapturedAt TIMESTAMP,
    snapshotData       TEXT,
    tblGroupid         VARCHAR(36)  NOT NULL,
    tblUserid          VARCHAR(36)  NOT NULL,
    CONSTRAINT pkGroupMember PRIMARY KEY (id),
    CONSTRAINT uqGroupMember UNIQUE (tblGroupid, tblUserid),
    CONSTRAINT fkGroupMemberGroup FOREIGN KEY (tblGroupid) REFERENCES tblGroup (id) ON DELETE CASCADE,
    CONSTRAINT fkGroupMemberUser FOREIGN KEY (tblUserid) REFERENCES tblUser (id),
    CONSTRAINT ckGroupMemberRole CHECK (role IN ('owner', 'admin', 'member')),
    CONSTRAINT ckGroupMemberStatus CHECK (status IN ('active', 'left')),
    CONSTRAINT ckGroupMemberLeftAt CHECK ((status = 'left') = (leftAt IS NOT NULL)),
    CONSTRAINT ckGroupMemberOwnerActive CHECK (role <> 'owner' OR status = 'active'),
    CONSTRAINT ckGroupMemberSnapshot CHECK ((snapshotData IS NULL) = (snapshotCapturedAt IS NULL))
);

CREATE TABLE tblInvitation (
    id            VARCHAR(36) NOT NULL,
    code          VARCHAR(12) NOT NULL,
    maxUses       INT,
    useCount      INT         NOT NULL DEFAULT 0,
    expiresAt     TIMESTAMP   NOT NULL,
    status        VARCHAR(10) NOT NULL DEFAULT 'active',
    createdAt     TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    tblGroupid    VARCHAR(36) NOT NULL,
    creatorUserid VARCHAR(36) NOT NULL,
    CONSTRAINT pkInvitation PRIMARY KEY (id),
    CONSTRAINT uqInvitationCode UNIQUE (code),
    CONSTRAINT fkInvitationGroup FOREIGN KEY (tblGroupid) REFERENCES tblGroup (id) ON DELETE CASCADE,
    CONSTRAINT fkInvitationCreator FOREIGN KEY (creatorUserid) REFERENCES tblUser (id),
    CONSTRAINT ckInvitationCode CHECK (code ~ '^[A-Z0-9]{8}$'),
    CONSTRAINT ckInvitationMaxUses CHECK (maxUses IS NULL OR maxUses >= 1),
    CONSTRAINT ckInvitationUseCount CHECK (useCount >= 0 AND (maxUses IS NULL OR useCount <= maxUses)),
    CONSTRAINT ckInvitationStatus CHECK (status IN ('active', 'revoked')),
    CONSTRAINT ckInvitationExpires CHECK (expiresAt > createdAt)
);

CREATE TABLE tblGroupExpense (
    id            VARCHAR(36)   NOT NULL,
    title         VARCHAR(100)  NOT NULL,
    totalAmount   DECIMAL(12,2) NOT NULL,
    occurredAt    TIMESTAMP     NOT NULL,
    category      VARCHAR(50)   NOT NULL,
    splitMethod   VARCHAR(10)   NOT NULL,
    note          VARCHAR(500),
    status        VARCHAR(10)   NOT NULL DEFAULT 'active',
    version       INT           NOT NULL DEFAULT 1,
    createdAt     TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updatedAt     TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    tblGroupid    VARCHAR(36)   NOT NULL,
    payerUserid   VARCHAR(36)   NOT NULL,
    creatorUserid VARCHAR(36)   NOT NULL,
    CONSTRAINT pkGroupExpense PRIMARY KEY (id),
    CONSTRAINT fkGroupExpenseGroup FOREIGN KEY (tblGroupid) REFERENCES tblGroup (id) ON DELETE CASCADE,
    CONSTRAINT fkGroupExpensePayer FOREIGN KEY (payerUserid) REFERENCES tblUser (id),
    CONSTRAINT fkGroupExpenseCreator FOREIGN KEY (creatorUserid) REFERENCES tblUser (id),
    CONSTRAINT ckGroupExpenseTitle CHECK (length(trim(title)) BETWEEN 1 AND 100),
    CONSTRAINT ckGroupExpenseTotal CHECK (totalAmount > 0),
    CONSTRAINT ckGroupExpenseCategory CHECK (length(trim(category)) BETWEEN 1 AND 50),
    CONSTRAINT ckGroupExpenseSplitMethod CHECK (splitMethod IN ('equal', 'exact')),
    CONSTRAINT ckGroupExpenseStatus CHECK (status IN ('active', 'cancelled')),
    CONSTRAINT ckGroupExpenseVersion CHECK (version >= 1)
);

-- Quan hệ n-n CÓ thuộc tính: phần chia của từng người
CREATE TABLE tblExpenseSplit (
    id                 VARCHAR(36)   NOT NULL,
    amount             DECIMAL(12,2) NOT NULL,
    roundingAdjustment DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    tblGroupExpenseid  VARCHAR(36)   NOT NULL,
    tblUserid          VARCHAR(36)   NOT NULL,
    CONSTRAINT pkExpenseSplit PRIMARY KEY (id),
    CONSTRAINT uqExpenseSplit UNIQUE (tblGroupExpenseid, tblUserid),
    CONSTRAINT fkExpenseSplitExpense FOREIGN KEY (tblGroupExpenseid) REFERENCES tblGroupExpense (id) ON DELETE CASCADE,
    CONSTRAINT fkExpenseSplitUser FOREIGN KEY (tblUserid) REFERENCES tblUser (id),
    CONSTRAINT ckExpenseSplitAmount CHECK (amount > 0),
    CONSTRAINT ckExpenseSplitRounding CHECK (roundingAdjustment >= 0)
);

-- Bút toán (lớp cha). tblExpenseSplitid khác NULL = shared entry; NULL = entry thường
CREATE TABLE tblEntry (
    id                  VARCHAR(36)  NOT NULL,
    note                VARCHAR(500),
    version             INT          NOT NULL DEFAULT 1,
    createdAt           TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updatedAt           TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    tblUserid           VARCHAR(36)  NOT NULL,
    tblCategoryid       VARCHAR(36)  NOT NULL,
    tblSpendingBucketid VARCHAR(36),
    tblExpenseSplitid   VARCHAR(36),
    CONSTRAINT pkEntry PRIMARY KEY (id),
    CONSTRAINT uqEntryExpenseSplit UNIQUE (tblExpenseSplitid),
    CONSTRAINT fkEntryUser FOREIGN KEY (tblUserid) REFERENCES tblUser (id) ON DELETE CASCADE,
    CONSTRAINT fkEntryCategory FOREIGN KEY (tblCategoryid) REFERENCES tblCategory (id),
    CONSTRAINT fkEntryBucket FOREIGN KEY (tblSpendingBucketid) REFERENCES tblSpendingBucket (id) ON DELETE SET NULL,
    CONSTRAINT fkEntryExpenseSplit FOREIGN KEY (tblExpenseSplitid) REFERENCES tblExpenseSplit (id),
    CONSTRAINT ckEntryVersion CHECK (version >= 1)
);

-- Khoản thu/chi thường (kế thừa tblEntry)
CREATE TABLE tblPersonalEntry (
    tblEntryid VARCHAR(36)   NOT NULL,
    entryType  VARCHAR(10)   NOT NULL,
    name       VARCHAR(100)  NOT NULL,
    amount     DECIMAL(12,2) NOT NULL,
    occurredAt TIMESTAMP     NOT NULL,
    CONSTRAINT pkPersonalEntry PRIMARY KEY (tblEntryid),
    CONSTRAINT fkPersonalEntryEntry FOREIGN KEY (tblEntryid) REFERENCES tblEntry (id) ON DELETE CASCADE,
    CONSTRAINT ckPersonalEntryType CHECK (entryType IN ('income', 'expense')),
    CONSTRAINT ckPersonalEntryName CHECK (length(trim(name)) BETWEEN 1 AND 100),
    CONSTRAINT ckPersonalEntryAmount CHECK (amount > 0)
);

CREATE TABLE tblRepayment (
    id          VARCHAR(36)   NOT NULL,
    amount      DECIMAL(12,2) NOT NULL,
    paidAt      TIMESTAMP     NOT NULL,
    note        VARCHAR(500),
    status      VARCHAR(12)   NOT NULL DEFAULT 'pending',
    respondedAt TIMESTAMP,
    createdAt   TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    tblGroupid  VARCHAR(36)   NOT NULL,
    fromUserid  VARCHAR(36)   NOT NULL,
    toUserid    VARCHAR(36)   NOT NULL,
    CONSTRAINT pkRepayment PRIMARY KEY (id),
    CONSTRAINT fkRepaymentGroup FOREIGN KEY (tblGroupid) REFERENCES tblGroup (id) ON DELETE CASCADE,
    CONSTRAINT fkRepaymentFrom FOREIGN KEY (fromUserid) REFERENCES tblUser (id),
    CONSTRAINT fkRepaymentTo FOREIGN KEY (toUserid) REFERENCES tblUser (id),
    CONSTRAINT ckRepaymentAmount CHECK (amount > 0),
    CONSTRAINT ckRepaymentStatus CHECK (status IN ('pending', 'confirmed', 'rejected', 'cancelled', 'reversed')),
    CONSTRAINT ckRepaymentPeople CHECK (fromUserid <> toUserid),
    CONSTRAINT ckRepaymentResponded CHECK ((status = 'pending') = (respondedAt IS NULL))
);

CREATE TABLE tblRepaymentReversal (
    id              VARCHAR(36)  NOT NULL,
    reason          VARCHAR(500) NOT NULL,
    status          VARCHAR(10)  NOT NULL DEFAULT 'pending',
    rejectionNote   VARCHAR(500),
    respondedAt     TIMESTAMP,
    createdAt       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    tblRepaymentid  VARCHAR(36)  NOT NULL,
    requesterUserid VARCHAR(36)  NOT NULL,
    approverUserid  VARCHAR(36)  NOT NULL,
    CONSTRAINT pkRepaymentReversal PRIMARY KEY (id),
    CONSTRAINT fkReversalRepayment FOREIGN KEY (tblRepaymentid) REFERENCES tblRepayment (id) ON DELETE CASCADE,
    CONSTRAINT fkReversalRequester FOREIGN KEY (requesterUserid) REFERENCES tblUser (id),
    CONSTRAINT fkReversalApprover FOREIGN KEY (approverUserid) REFERENCES tblUser (id),
    CONSTRAINT ckReversalReason CHECK (length(trim(reason)) BETWEEN 1 AND 500),
    CONSTRAINT ckReversalStatus CHECK (status IN ('pending', 'approved', 'rejected')),
    CONSTRAINT ckReversalPeople CHECK (requesterUserid <> approverUserid),
    CONSTRAINT ckReversalNote CHECK (rejectionNote IS NULL OR status = 'rejected')
);

CREATE TABLE tblGroupActivity (
    id           VARCHAR(36) NOT NULL,
    activityType VARCHAR(40) NOT NULL,
    targetType   VARCHAR(30),
    targetId     VARCHAR(36),
    payload      TEXT        NOT NULL DEFAULT '{}',
    createdAt    TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    tblGroupid   VARCHAR(36) NOT NULL,
    actorUserid  VARCHAR(36),
    CONSTRAINT pkGroupActivity PRIMARY KEY (id),
    CONSTRAINT fkActivityGroup FOREIGN KEY (tblGroupid) REFERENCES tblGroup (id) ON DELETE CASCADE,
    CONSTRAINT fkActivityActor FOREIGN KEY (actorUserid) REFERENCES tblUser (id) ON DELETE SET NULL
);

-- ----------------------------- PHẦN CORRECTION -----------------------

CREATE TABLE tblCorrectionRequest (
    id                  VARCHAR(36)   NOT NULL,
    requestType         VARCHAR(12)   NOT NULL,
    reason              VARCHAR(500),
    status              VARCHAR(10)   NOT NULL DEFAULT 'pending',
    baseVersion         INT           NOT NULL,
    previousTitle       VARCHAR(100)  NOT NULL,
    proposedTitle       VARCHAR(100),
    previousTotalAmount DECIMAL(12,2) NOT NULL,
    proposedTotalAmount DECIMAL(12,2),
    previousOccurredAt  TIMESTAMP     NOT NULL,
    proposedOccurredAt  TIMESTAMP,
    previousCategory    VARCHAR(50)   NOT NULL,
    proposedCategory    VARCHAR(50),
    previousSplitMethod VARCHAR(10)   NOT NULL,
    proposedSplitMethod VARCHAR(10),
    createdAt           TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    closedAt            TIMESTAMP,
    tblGroupExpenseid   VARCHAR(36)   NOT NULL,
    requesterUserid     VARCHAR(36)   NOT NULL,
    previousPayerUserid VARCHAR(36)   NOT NULL,
    proposedPayerUserid VARCHAR(36),
    CONSTRAINT pkCorrectionRequest PRIMARY KEY (id),
    CONSTRAINT fkCorrectionExpense FOREIGN KEY (tblGroupExpenseid) REFERENCES tblGroupExpense (id) ON DELETE CASCADE,
    CONSTRAINT fkCorrectionRequester FOREIGN KEY (requesterUserid) REFERENCES tblUser (id),
    CONSTRAINT fkCorrectionPreviousPayer FOREIGN KEY (previousPayerUserid) REFERENCES tblUser (id),
    CONSTRAINT fkCorrectionProposedPayer FOREIGN KEY (proposedPayerUserid) REFERENCES tblUser (id),
    CONSTRAINT ckCorrectionType CHECK (requestType IN ('correction', 'cancellation')),
    CONSTRAINT ckCorrectionReason CHECK ((reason IS NULL AND requestType = 'cancellation') OR length(trim(reason)) BETWEEN 1 AND 500),
    CONSTRAINT ckCorrectionStatus CHECK (status IN ('pending', 'completed', 'rejected', 'withdrawn')),
    CONSTRAINT ckCorrectionClosedAt CHECK ((status = 'pending') = (closedAt IS NULL)),
    CONSTRAINT ckCorrectionAmounts CHECK (previousTotalAmount > 0 AND (proposedTotalAmount IS NULL OR proposedTotalAmount > 0)),
    CONSTRAINT ckCorrectionSplitMethod CHECK (previousSplitMethod IN ('equal', 'exact')
        AND (proposedSplitMethod IS NULL OR proposedSplitMethod IN ('equal', 'exact'))),
    CONSTRAINT ckCorrectionCancellation CHECK (requestType = 'correction' OR (
        proposedTitle IS NULL AND proposedTotalAmount IS NULL AND proposedOccurredAt IS NULL
        AND proposedCategory IS NULL AND proposedSplitMethod IS NULL AND proposedPayerUserid IS NULL))
);

-- Gộp CorrectionShare + CorrectionApproval (1-1 trên cùng cặp request - user)
CREATE TABLE tblCorrectionParticipant (
    id                     VARCHAR(36)   NOT NULL,
    previousAmount         DECIMAL(12,2),
    proposedAmount         DECIMAL(12,2),
    approvalStatus         VARCHAR(12),
    respondedAt            TIMESTAMP,
    rejectionNote          VARCHAR(500),
    tblCorrectionRequestid VARCHAR(36)   NOT NULL,
    tblUserid              VARCHAR(36)   NOT NULL,
    CONSTRAINT pkCorrectionParticipant PRIMARY KEY (id),
    CONSTRAINT uqCorrectionParticipant UNIQUE (tblCorrectionRequestid, tblUserid),
    CONSTRAINT fkParticipantRequest FOREIGN KEY (tblCorrectionRequestid) REFERENCES tblCorrectionRequest (id) ON DELETE CASCADE,
    CONSTRAINT fkParticipantUser FOREIGN KEY (tblUserid) REFERENCES tblUser (id),
    CONSTRAINT ckParticipantAmounts CHECK ((previousAmount IS NULL OR previousAmount >= 0)
        AND (proposedAmount IS NULL OR proposedAmount > 0)),
    CONSTRAINT ckParticipantStatus CHECK (approvalStatus IS NULL
        OR approvalStatus IN ('pending', 'approved', 'rejected', 'no_response')),
    CONSTRAINT ckParticipantResponded CHECK ((approvalStatus IN ('approved', 'rejected')) IS NOT DISTINCT FROM (respondedAt IS NOT NULL)
        OR (approvalStatus IS NULL AND respondedAt IS NULL)),
    CONSTRAINT ckParticipantNote CHECK (rejectionNote IS NULL OR approvalStatus = 'rejected')
);

-- --------------------------------- KHÁC ------------------------------

CREATE TABLE tblDeletionMarker (
    id         VARCHAR(36) NOT NULL,
    recordType VARCHAR(20) NOT NULL,
    recordId   VARCHAR(36) NOT NULL,
    deletedAt  TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expiresAt  TIMESTAMP   NOT NULL,
    tblUserid  VARCHAR(36) NOT NULL,
    CONSTRAINT pkDeletionMarker PRIMARY KEY (id),
    CONSTRAINT uqDeletionMarker UNIQUE (tblUserid, recordType, recordId),
    CONSTRAINT fkDeletionMarkerUser FOREIGN KEY (tblUserid) REFERENCES tblUser (id) ON DELETE CASCADE,
    CONSTRAINT ckDeletionMarkerType CHECK (recordType IN ('entry', 'category', 'bucket')),
    CONSTRAINT ckDeletionMarkerExpires CHECK (expiresAt > deletedAt)
);

CREATE TABLE tblNotification (
    id               VARCHAR(36) NOT NULL,
    notificationType VARCHAR(40) NOT NULL,
    payload          TEXT        NOT NULL DEFAULT '{}',
    readAt           TIMESTAMP,
    createdAt        TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    tblUserid        VARCHAR(36) NOT NULL,
    CONSTRAINT pkNotification PRIMARY KEY (id),
    CONSTRAINT fkNotificationUser FOREIGN KEY (tblUserid) REFERENCES tblUser (id) ON DELETE CASCADE
);

-- =====================================================================
-- 2. INDEX
-- =====================================================================

-- UNIQUE không phân biệt hoa thường: "Shopping" = "shopping", "A@x.com" = "a@x.com"
CREATE UNIQUE INDEX uqUserEmailLower      ON tblUser (lower(email));
CREATE UNIQUE INDEX uqCategoryNameLower   ON tblCategory (tblUserid, lower(name));
-- Mỗi user đúng 1 danh mục hệ thống (Uncategorized)
CREATE UNIQUE INDEX uqCategorySystem      ON tblCategory (tblUserid) WHERE isSystem;
-- Mỗi nhóm đúng 1 owner
CREATE UNIQUE INDEX uqGroupOwner          ON tblGroupMember (tblGroupid) WHERE role = 'owner';
-- Mỗi hóa đơn tối đa 1 yêu cầu sửa đang chờ
CREATE UNIQUE INDEX uqCorrectionPending   ON tblCorrectionRequest (tblGroupExpenseid) WHERE status = 'pending';
-- Mỗi khoản trả nợ tối đa 1 yêu cầu đảo đang chờ
CREATE UNIQUE INDEX uqReversalPending     ON tblRepaymentReversal (tblRepaymentid) WHERE status = 'pending';

-- Index tăng tốc truy vấn thường dùng
CREATE INDEX ixEntryUser          ON tblEntry (tblUserid, createdAt DESC);
CREATE INDEX ixEntryBucket        ON tblEntry (tblSpendingBucketid);
CREATE INDEX ixPersonalEntryDate  ON tblPersonalEntry (occurredAt);
CREATE INDEX ixGroupExpenseGroup  ON tblGroupExpense (tblGroupid, occurredAt DESC);
CREATE INDEX ixExpenseSplitUser   ON tblExpenseSplit (tblUserid);
CREATE INDEX ixGroupMemberUser    ON tblGroupMember (tblUserid);
CREATE INDEX ixRepaymentGroup     ON tblRepayment (tblGroupid, status);
CREATE INDEX ixActivityGroup      ON tblGroupActivity (tblGroupid, createdAt DESC);
CREATE INDEX ixNotificationUser   ON tblNotification (tblUserid, createdAt DESC);

-- =====================================================================
-- 3. TRIGGER (ràng buộc liên bảng không viết được bằng CHECK)
-- =====================================================================

-- 3.1 Tự cập nhật updatedAt khi sửa
CREATE OR REPLACE FUNCTION fnSetUpdatedAt() RETURNS trigger AS $$
BEGIN
    NEW.updatedAt := CURRENT_TIMESTAMP;
    RETURN NEW;
END $$ LANGUAGE plpgsql;

CREATE TRIGGER trgCategoryUpdatedAt     BEFORE UPDATE ON tblCategory       FOR EACH ROW EXECUTE FUNCTION fnSetUpdatedAt();
CREATE TRIGGER trgBucketUpdatedAt       BEFORE UPDATE ON tblSpendingBucket FOR EACH ROW EXECUTE FUNCTION fnSetUpdatedAt();
CREATE TRIGGER trgEntryUpdatedAt        BEFORE UPDATE ON tblEntry          FOR EACH ROW EXECUTE FUNCTION fnSetUpdatedAt();
CREATE TRIGGER trgGroupExpenseUpdatedAt BEFORE UPDATE ON tblGroupExpense   FOR EACH ROW EXECUTE FUNCTION fnSetUpdatedAt();

-- 3.2 Tổng phần chia = tổng hóa đơn (kiểm tra lúc COMMIT để chèn hóa đơn và phần chia trong cùng transaction)
CREATE OR REPLACE FUNCTION fnCheckSplitTotal() RETURNS trigger AS $$
DECLARE
    vExpense VARCHAR(36);
    vTotal   DECIMAL(12,2);
    vSum     DECIMAL(12,2);
BEGIN
    IF TG_TABLE_NAME = 'tblgroupexpense' THEN
        vExpense := NEW.id;
    ELSE
        vExpense := COALESCE(NEW.tblGroupExpenseid, OLD.tblGroupExpenseid);
    END IF;
    SELECT totalAmount INTO vTotal FROM tblGroupExpense WHERE id = vExpense;
    IF vTotal IS NULL THEN
        RETURN NULL;                                   -- hóa đơn đã bị xóa
    END IF;
    SELECT COALESCE(SUM(amount), 0) INTO vSum FROM tblExpenseSplit WHERE tblGroupExpenseid = vExpense;
    IF vSum <> vTotal THEN
        RAISE EXCEPTION 'SPLIT_TOTAL_MISMATCH: hóa đơn % có tổng % nhưng các phần chia cộng lại %', vExpense, vTotal, vSum;
    END IF;
    RETURN NULL;
END $$ LANGUAGE plpgsql;

CREATE CONSTRAINT TRIGGER trgSplitTotal
    AFTER INSERT OR UPDATE OR DELETE ON tblExpenseSplit
    DEFERRABLE INITIALLY DEFERRED FOR EACH ROW EXECUTE FUNCTION fnCheckSplitTotal();
CREATE CONSTRAINT TRIGGER trgExpenseTotal
    AFTER INSERT OR UPDATE OF totalAmount ON tblGroupExpense
    DEFERRABLE INITIALLY DEFERRED FOR EACH ROW EXECUTE FUNCTION fnCheckSplitTotal();

-- 3.3 Bucket và danh mục trong tblBucketCategory phải cùng một user
CREATE OR REPLACE FUNCTION fnCheckBucketCategory() RETURNS trigger AS $$
BEGIN
    IF (SELECT tblUserid FROM tblSpendingBucket WHERE id = NEW.tblSpendingBucketid)
       <> (SELECT tblUserid FROM tblCategory WHERE id = NEW.tblCategoryid) THEN
        RAISE EXCEPTION 'Bucket và danh mục phải thuộc cùng một người dùng';
    END IF;
    RETURN NEW;
END $$ LANGUAGE plpgsql;

CREATE TRIGGER trgBucketCategoryOwner BEFORE INSERT OR UPDATE ON tblBucketCategory
    FOR EACH ROW EXECUTE FUNCTION fnCheckBucketCategory();

-- 3.4 tblEntry: danh mục, bucket, phần chia phải thuộc đúng chủ bút toán; bucket phải theo dõi danh mục
CREATE OR REPLACE FUNCTION fnCheckEntry() RETURNS trigger AS $$
BEGIN
    IF (SELECT tblUserid FROM tblCategory WHERE id = NEW.tblCategoryid) <> NEW.tblUserid THEN
        RAISE EXCEPTION 'CATEGORY_NOT_FOUND: danh mục không thuộc người dùng';
    END IF;
    IF NEW.tblSpendingBucketid IS NOT NULL THEN
        IF (SELECT tblUserid FROM tblSpendingBucket WHERE id = NEW.tblSpendingBucketid) <> NEW.tblUserid THEN
            RAISE EXCEPTION 'Bucket không thuộc người dùng';
        END IF;
        IF NOT EXISTS (SELECT 1 FROM tblBucketCategory
                       WHERE tblSpendingBucketid = NEW.tblSpendingBucketid AND tblCategoryid = NEW.tblCategoryid) THEN
            RAISE EXCEPTION 'BUCKET_CATEGORY_MISMATCH: bucket không theo dõi danh mục này';
        END IF;
    END IF;
    IF NEW.tblExpenseSplitid IS NOT NULL
       AND (SELECT tblUserid FROM tblExpenseSplit WHERE id = NEW.tblExpenseSplitid) <> NEW.tblUserid THEN
        RAISE EXCEPTION 'Shared entry phải trỏ tới phần chia của chính người dùng';
    END IF;
    RETURN NEW;
END $$ LANGUAGE plpgsql;

CREATE TRIGGER trgEntryCheck BEFORE INSERT OR UPDATE ON tblEntry
    FOR EACH ROW EXECUTE FUNCTION fnCheckEntry();

-- 3.5 tblPersonalEntry chỉ dành cho entry thường; khoản thu không thuộc bucket
CREATE OR REPLACE FUNCTION fnCheckPersonalEntry() RETURNS trigger AS $$
DECLARE
    vSplit  VARCHAR(36);
    vBucket VARCHAR(36);
BEGIN
    SELECT tblExpenseSplitid, tblSpendingBucketid INTO vSplit, vBucket FROM tblEntry WHERE id = NEW.tblEntryid;
    IF vSplit IS NOT NULL THEN
        RAISE EXCEPTION 'Shared entry không có dòng ở tblPersonalEntry';
    END IF;
    IF NEW.entryType = 'income' AND vBucket IS NOT NULL THEN
        RAISE EXCEPTION 'BUCKET_NOT_ALLOWED_FOR_INCOME: khoản thu không thuộc bucket';
    END IF;
    RETURN NEW;
END $$ LANGUAGE plpgsql;

CREATE TRIGGER trgPersonalEntryCheck BEFORE INSERT OR UPDATE ON tblPersonalEntry
    FOR EACH ROW EXECUTE FUNCTION fnCheckPersonalEntry();

-- 3.6 Chỉ người tạo hóa đơn mới được gửi yêu cầu sửa
CREATE OR REPLACE FUNCTION fnCheckCorrectionRequester() RETURNS trigger AS $$
BEGIN
    IF (SELECT creatorUserid FROM tblGroupExpense WHERE id = NEW.tblGroupExpenseid) <> NEW.requesterUserid THEN
        RAISE EXCEPTION 'NOT_EXPENSE_CREATOR: chỉ người tạo hóa đơn mới được yêu cầu sửa';
    END IF;
    RETURN NEW;
END $$ LANGUAGE plpgsql;

CREATE TRIGGER trgCorrectionRequester BEFORE INSERT ON tblCorrectionRequest
    FOR EACH ROW EXECUTE FUNCTION fnCheckCorrectionRequester();

-- =====================================================================
-- 4. VIEW - giá trị dẫn xuất (thay cho các lớp thống kê TK... đã loại bỏ)
-- =====================================================================

-- 4.1 Số dư của từng thành viên trong nhóm (dương = được nhận, âm = đang nợ)
CREATE VIEW vGroupBalance AS
SELECT m.tblGroupid, m.tblUserid,
      COALESCE((SELECT SUM(e.totalAmount) FROM tblGroupExpense e
                WHERE e.tblGroupid = m.tblGroupid AND e.payerUserid = m.tblUserid AND e.status = 'active'), 0)
    - COALESCE((SELECT SUM(s.amount) FROM tblExpenseSplit s JOIN tblGroupExpense e ON e.id = s.tblGroupExpenseid
                WHERE e.tblGroupid = m.tblGroupid AND s.tblUserid = m.tblUserid AND e.status = 'active'), 0)
    + COALESCE((SELECT SUM(r.amount) FROM tblRepayment r
                WHERE r.tblGroupid = m.tblGroupid AND r.fromUserid = m.tblUserid AND r.status = 'confirmed'), 0)
    - COALESCE((SELECT SUM(r.amount) FROM tblRepayment r
                WHERE r.tblGroupid = m.tblGroupid AND r.toUserid = m.tblUserid AND r.status = 'confirmed'), 0)
    AS balance
FROM tblGroupMember m;

-- 4.2 Bút toán đầy đủ: entry thường lấy từ tblPersonalEntry, shared entry lấy từ phần chia và hóa đơn
-- note/version/createdAt/updatedAt/groupExpenseId được thêm để phục vụ API GET /entries (section 4.1-4.4)
CREATE VIEW vEntryDetail AS
SELECT en.id, en.tblUserid, en.tblCategoryid, en.tblSpendingBucketid,
       p.entryType, p.name, p.amount, p.occurredAt, FALSE AS isShared, NULL::VARCHAR(36) AS tblGroupid,
       en.note, en.version, en.createdAt, en.updatedAt, NULL::VARCHAR(36) AS tblGroupExpenseid
FROM tblEntry en JOIN tblPersonalEntry p ON p.tblEntryid = en.id
UNION ALL
SELECT en.id, en.tblUserid, en.tblCategoryid, en.tblSpendingBucketid,
       'expense', ge.title, s.amount, ge.occurredAt, TRUE, ge.tblGroupid,
       en.note, en.version, en.createdAt, en.updatedAt, ge.id
FROM tblEntry en
JOIN tblExpenseSplit s  ON s.id = en.tblExpenseSplitid
JOIN tblGroupExpense ge ON ge.id = s.tblGroupExpenseid AND ge.status = 'active';

-- 4.3 Chi tiêu của bucket theo từng tháng
CREATE VIEW vBucketMonthlySpending AS
SELECT b.id AS tblSpendingBucketid, b.tblUserid, b.name, b.monthlyTarget,
       to_char(d.occurredAt, 'YYYY-MM') AS month,
       SUM(d.amount) AS spent,
       b.monthlyTarget - SUM(d.amount) AS remaining,
       ROUND(100 * SUM(d.amount) / b.monthlyTarget, 1) AS progressPercent
FROM tblSpendingBucket b
JOIN vEntryDetail d ON d.tblSpendingBucketid = b.id AND d.entryType = 'expense'
GROUP BY b.id, b.tblUserid, b.name, b.monthlyTarget, to_char(d.occurredAt, 'YYYY-MM');

-- =====================================================================
-- 5. DỮ LIỆU MẪU
-- Kịch bản đặc tả: Alice trả 120.00 USD bữa tối, chia đều cho Alice, Bob, Cara;
-- Bob đã trả lại 40.00 USD; Alice đề xuất sửa hóa đơn thành 150.00 USD (đang chờ duyệt).
-- =====================================================================

INSERT INTO tblUser (id, email, passwordHash, displayName, timezone) VALUES
 ('a0000000-0000-4000-8000-000000000001', 'alice@example.com', '$2b$10$hashAlice', 'Alice', 'Asia/Ho_Chi_Minh'),
 ('a0000000-0000-4000-8000-000000000002', 'bob@example.com',   '$2b$10$hashBob',   'Bob',   'Asia/Ho_Chi_Minh'),
 ('a0000000-0000-4000-8000-000000000003', 'cara@example.com',  '$2b$10$hashCara',  'Cara',  'Asia/Ho_Chi_Minh');

INSERT INTO tblDevice (id, platform, lastSyncAt, tblUserid) VALUES
 ('d0000000-0000-4000-8000-000000000001', 'android', '2026-10-31 08:00', 'a0000000-0000-4000-8000-000000000001');

-- Danh mục (mỗi user có Uncategorized là danh mục hệ thống)
INSERT INTO tblCategory (id, name, isSystem, tblUserid) VALUES
 ('c0000000-0000-4000-8000-000000000101', 'Uncategorized', TRUE,  'a0000000-0000-4000-8000-000000000001'),
 ('c0000000-0000-4000-8000-000000000102', 'Food',          FALSE, 'a0000000-0000-4000-8000-000000000001'),
 ('c0000000-0000-4000-8000-000000000103', 'Groceries',     FALSE, 'a0000000-0000-4000-8000-000000000001'),
 ('c0000000-0000-4000-8000-000000000104', 'Salary',        FALSE, 'a0000000-0000-4000-8000-000000000001'),
 ('c0000000-0000-4000-8000-000000000201', 'Uncategorized', TRUE,  'a0000000-0000-4000-8000-000000000002'),
 ('c0000000-0000-4000-8000-000000000202', 'Eating out',    FALSE, 'a0000000-0000-4000-8000-000000000002'),
 ('c0000000-0000-4000-8000-000000000301', 'Uncategorized', TRUE,  'a0000000-0000-4000-8000-000000000003'),
 ('c0000000-0000-4000-8000-000000000302', 'Food',          FALSE, 'a0000000-0000-4000-8000-000000000003');

-- Bucket Food của Alice, theo dõi Food và Groceries (quan hệ n-n không thuộc tính)
INSERT INTO tblSpendingBucket (id, name, monthlyTarget, tblUserid) VALUES
 ('b0000000-0000-4000-8000-000000000101', 'Food', 300.00, 'a0000000-0000-4000-8000-000000000001');
INSERT INTO tblBucketCategory (tblSpendingBucketid, tblCategoryid) VALUES
 ('b0000000-0000-4000-8000-000000000101', 'c0000000-0000-4000-8000-000000000102'),
 ('b0000000-0000-4000-8000-000000000101', 'c0000000-0000-4000-8000-000000000103');

-- Khoản thu/chi thường của Alice
INSERT INTO tblEntry (id, note, tblUserid, tblCategoryid, tblSpendingBucketid) VALUES
 ('e0000000-0000-4000-8000-000000000101', NULL,             'a0000000-0000-4000-8000-000000000001', 'c0000000-0000-4000-8000-000000000104', NULL),
 ('e0000000-0000-4000-8000-000000000102', 'Weekly shopping','a0000000-0000-4000-8000-000000000001', 'c0000000-0000-4000-8000-000000000103', 'b0000000-0000-4000-8000-000000000101');
INSERT INTO tblPersonalEntry (tblEntryid, entryType, name, amount, occurredAt) VALUES
 ('e0000000-0000-4000-8000-000000000101', 'income',  'October salary', 1500.00, '2026-10-01 09:00'),
 ('e0000000-0000-4000-8000-000000000102', 'expense', 'Groceries',        62.40, '2026-10-18 18:05');

-- Nhóm và thành viên (quan hệ n-n có thuộc tính)
INSERT INTO tblGroup (id, name, description, creatorUserid) VALUES
 ('g0000000-0000-4000-8000-000000000001', 'Da Lat Trip', 'Nov 2026', 'a0000000-0000-4000-8000-000000000001');
INSERT INTO tblGroupMember (id, role, tblGroupid, tblUserid) VALUES
 ('m0000000-0000-4000-8000-000000000001', 'owner',  'g0000000-0000-4000-8000-000000000001', 'a0000000-0000-4000-8000-000000000001'),
 ('m0000000-0000-4000-8000-000000000002', 'member', 'g0000000-0000-4000-8000-000000000001', 'a0000000-0000-4000-8000-000000000002'),
 ('m0000000-0000-4000-8000-000000000003', 'member', 'g0000000-0000-4000-8000-000000000001', 'a0000000-0000-4000-8000-000000000003');
INSERT INTO tblInvitation (id, code, expiresAt, tblGroupid, creatorUserid) VALUES
 ('i0000000-0000-4000-8000-000000000001', 'K7Q2M9XA', CURRENT_TIMESTAMP + INTERVAL '7 days',
  'g0000000-0000-4000-8000-000000000001', 'a0000000-0000-4000-8000-000000000001');

-- Hóa đơn chung 120.00, chia đều 40.00 x 3 (quan hệ n-n có thuộc tính)
INSERT INTO tblGroupExpense (id, title, totalAmount, occurredAt, category, splitMethod, tblGroupid, payerUserid, creatorUserid) VALUES
 ('x0000000-0000-4000-8000-000000000001', 'Dinner', 120.00, '2026-10-20 19:30', 'Food', 'equal',
  'g0000000-0000-4000-8000-000000000001', 'a0000000-0000-4000-8000-000000000001', 'a0000000-0000-4000-8000-000000000001');
INSERT INTO tblExpenseSplit (id, amount, tblGroupExpenseid, tblUserid) VALUES
 ('s0000000-0000-4000-8000-000000000001', 40.00, 'x0000000-0000-4000-8000-000000000001', 'a0000000-0000-4000-8000-000000000001'),
 ('s0000000-0000-4000-8000-000000000002', 40.00, 'x0000000-0000-4000-8000-000000000001', 'a0000000-0000-4000-8000-000000000002'),
 ('s0000000-0000-4000-8000-000000000003', 40.00, 'x0000000-0000-4000-8000-000000000001', 'a0000000-0000-4000-8000-000000000003');

-- Shared entry trong personal log mỗi người (Bob tự đổi danh mục thành "Eating out")
INSERT INTO tblEntry (id, tblUserid, tblCategoryid, tblSpendingBucketid, tblExpenseSplitid) VALUES
 ('e0000000-0000-4000-8000-000000000201', 'a0000000-0000-4000-8000-000000000001', 'c0000000-0000-4000-8000-000000000102',
  'b0000000-0000-4000-8000-000000000101', 's0000000-0000-4000-8000-000000000001'),
 ('e0000000-0000-4000-8000-000000000202', 'a0000000-0000-4000-8000-000000000002', 'c0000000-0000-4000-8000-000000000202',
  NULL, 's0000000-0000-4000-8000-000000000002'),
 ('e0000000-0000-4000-8000-000000000203', 'a0000000-0000-4000-8000-000000000003', 'c0000000-0000-4000-8000-000000000302',
  NULL, 's0000000-0000-4000-8000-000000000003');

-- Bob trả Alice 40.00, Alice đã xác nhận
INSERT INTO tblRepayment (id, amount, paidAt, note, status, respondedAt, tblGroupid, fromUserid, toUserid) VALUES
 ('r0000000-0000-4000-8000-000000000001', 40.00, '2026-10-21 09:00', 'Bank transfer', 'confirmed', '2026-10-21 10:00',
  'g0000000-0000-4000-8000-000000000001', 'a0000000-0000-4000-8000-000000000002', 'a0000000-0000-4000-8000-000000000001');

-- Alice đề xuất sửa 120.00 -> 150.00 (đang chờ Bob và Cara duyệt)
INSERT INTO tblCorrectionRequest (id, requestType, reason, baseVersion,
    previousTitle, previousTotalAmount, proposedTotalAmount, previousOccurredAt, previousCategory, previousSplitMethod,
    tblGroupExpenseid, requesterUserid, previousPayerUserid) VALUES
 ('q0000000-0000-4000-8000-000000000001', 'correction', 'Forgot the drinks on the receipt', 1,
  'Dinner', 120.00, 150.00, '2026-10-20 19:30', 'Food', 'equal',
  'x0000000-0000-4000-8000-000000000001', 'a0000000-0000-4000-8000-000000000001', 'a0000000-0000-4000-8000-000000000001');
INSERT INTO tblCorrectionParticipant (id, previousAmount, proposedAmount, approvalStatus, respondedAt, tblCorrectionRequestid, tblUserid) VALUES
 ('p0000000-0000-4000-8000-000000000001', 40.00, 50.00, 'approved', '2026-10-22 10:00', 'q0000000-0000-4000-8000-000000000001', 'a0000000-0000-4000-8000-000000000001'),
 ('p0000000-0000-4000-8000-000000000002', 40.00, 50.00, 'pending',  NULL,               'q0000000-0000-4000-8000-000000000001', 'a0000000-0000-4000-8000-000000000002'),
 ('p0000000-0000-4000-8000-000000000003', 40.00, 50.00, 'pending',  NULL,               'q0000000-0000-4000-8000-000000000001', 'a0000000-0000-4000-8000-000000000003');

INSERT INTO tblGroupActivity (id, activityType, targetType, targetId, payload, tblGroupid, actorUserid) VALUES
 ('h0000000-0000-4000-8000-000000000001', 'expense_created', 'expense', 'x0000000-0000-4000-8000-000000000001',
  '{"title":"Dinner","totalAmount":"120.00"}', 'g0000000-0000-4000-8000-000000000001', 'a0000000-0000-4000-8000-000000000001'),
 ('h0000000-0000-4000-8000-000000000002', 'repayment_confirmed', 'repayment', 'r0000000-0000-4000-8000-000000000001',
  '{"amount":"40.00"}', 'g0000000-0000-4000-8000-000000000001', 'a0000000-0000-4000-8000-000000000001');

INSERT INTO tblNotification (id, notificationType, payload, tblUserid) VALUES
 ('n0000000-0000-4000-8000-000000000001', 'correction_requested', '{"correctionId":"q0000000-0000-4000-8000-000000000001"}',
  'a0000000-0000-4000-8000-000000000002');


-- =====================================================================
-- 6. TRUY VẤN KIỂM TRA (chạy thử sau khi tạo)
-- =====================================================================
-- Số dư nhóm: Alice +40.00, Bob 0.00, Cara -40.00
--   SELECT u.displayName, b.balance FROM vGroupBalance b JOIN tblUser u ON u.id = b.tblUserid ORDER BY u.displayName;
-- Chi tiêu bucket Food của Alice tháng 10/2026: 62.40 + 40.00 = 102.40 / 300.00
--   SELECT * FROM vBucketMonthlySpending;
-- Lịch sử thu/chi của Bob (gồm phần chi nhóm 40.00)
--   SELECT * FROM vEntryDetail WHERE tblUserid = 'a0000000-0000-4000-8000-000000000002';

-- request idempotency
CREATE TABLE api_idempotency (
    user_id VARCHAR(36) NOT NULL REFERENCES tblUser(id) ON DELETE CASCADE,
    request_key VARCHAR(64) NOT NULL,
    fingerprint VARCHAR(64) NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    status INTEGER,
    response TEXT,
    PRIMARY KEY (user_id, request_key)
);

CREATE INDEX api_idempotency_expiry_idx ON api_idempotency (expires_at);

-- personal sync
CREATE TABLE personal_sync_change (
    sequence BIGSERIAL PRIMARY KEY,
    change_tx xid8 NOT NULL DEFAULT pg_current_xact_id(),
    user_id VARCHAR(36) NOT NULL REFERENCES tblUser(id) ON DELETE CASCADE,
    record_type VARCHAR(20) NOT NULL,
    record_id VARCHAR(36) NOT NULL,
    UNIQUE (user_id, record_type, record_id)
);

CREATE INDEX personal_sync_page_idx ON personal_sync_change (user_id, change_tx, sequence);

ALTER TABLE tblDevice ADD COLUMN sync_cursor TEXT;

INSERT INTO personal_sync_change (user_id, record_type, record_id)
SELECT tblUserid, 'entry', id FROM tblEntry
UNION ALL SELECT tblUserid, 'category', id FROM tblCategory
UNION ALL SELECT tblUserid, 'bucket', id FROM tblSpendingBucket
UNION ALL SELECT tblUserid, recordType, recordId FROM tblDeletionMarker
ON CONFLICT (user_id, record_type, record_id) DO NOTHING;

CREATE FUNCTION record_personal_sync_change() RETURNS TRIGGER LANGUAGE plpgsql AS $$
DECLARE
    owner_id varchar;
    item_type varchar;
    item_id varchar;
BEGIN
    IF TG_TABLE_NAME = 'tbldeletionmarker' THEN
        owner_id := NEW.tblUserid;
        item_type := NEW.recordType;
        item_id := NEW.recordId;
    ELSIF TG_TABLE_NAME = 'tblbucketcategory' THEN
        item_id := CASE WHEN TG_OP = 'DELETE' THEN OLD.tblSpendingBucketid ELSE NEW.tblSpendingBucketid END;
        SELECT tblUserid INTO owner_id FROM tblSpendingBucket WHERE id = item_id;
        item_type := 'bucket';
    ELSE
        owner_id := NEW.tblUserid;
        item_id := NEW.id;
        item_type := TG_ARGV[0];
    END IF;
    IF owner_id IS NOT NULL THEN
        INSERT INTO personal_sync_change (user_id, record_type, record_id)
        VALUES (owner_id, item_type, item_id)
        ON CONFLICT (user_id, record_type, record_id)
        DO UPDATE SET change_tx = EXCLUDED.change_tx, sequence = EXCLUDED.sequence;
    END IF;
    RETURN NULL;
END;
$$;

CREATE TRIGGER sync_entry AFTER INSERT OR UPDATE ON tblEntry
    FOR EACH ROW EXECUTE FUNCTION record_personal_sync_change('entry');
CREATE TRIGGER sync_category AFTER INSERT OR UPDATE ON tblCategory
    FOR EACH ROW EXECUTE FUNCTION record_personal_sync_change('category');
CREATE TRIGGER sync_bucket AFTER INSERT OR UPDATE ON tblSpendingBucket
    FOR EACH ROW EXECUTE FUNCTION record_personal_sync_change('bucket');
CREATE TRIGGER sync_bucket_categories AFTER INSERT OR DELETE ON tblBucketCategory
    FOR EACH ROW EXECUTE FUNCTION record_personal_sync_change();
CREATE TRIGGER sync_marker AFTER INSERT OR UPDATE ON tblDeletionMarker
    FOR EACH ROW EXECUTE FUNCTION record_personal_sync_change();

CREATE TABLE sync_push_job (
    id UUID PRIMARY KEY,
    user_id VARCHAR(36) NOT NULL REFERENCES tblUser(id) ON DELETE CASCADE,
    request_key VARCHAR(64) NOT NULL,
    fingerprint VARCHAR(64) NOT NULL,
    cursor TEXT NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    UNIQUE (user_id, request_key)
);

CREATE TABLE sync_push_result (
    job_id UUID NOT NULL REFERENCES sync_push_job(id) ON DELETE CASCADE,
    position INTEGER NOT NULL,
    response TEXT NOT NULL,
    PRIMARY KEY (job_id, position)
);

CREATE INDEX sync_push_job_expiry_idx ON sync_push_job (expires_at);

-- auth throttle
CREATE TABLE auth_throttle (
    scope VARCHAR(16) NOT NULL,
    identity TEXT NOT NULL,
    attempts TIMESTAMPTZ[] NOT NULL DEFAULT '{}',
    touched_at TIMESTAMPTZ NOT NULL DEFAULT clock_timestamp(),
    PRIMARY KEY (scope, identity)
);

CREATE INDEX auth_throttle_touched_idx ON auth_throttle (touched_at);
