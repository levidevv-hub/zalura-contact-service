DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM contacts c
        JOIN contacts existing
          ON existing.company_id = c.company_id
         AND existing.phone = '55' || c.phone
         AND existing.id <> c.id
        WHERE LENGTH(c.phone) = 11
    ) THEN
        RAISE EXCEPTION
            'Existem telefones duplicados após normalização';
END IF;
END $$;

UPDATE contacts
SET phone = '55' || phone,
    updated_at = NOW()
WHERE LENGTH(phone) = 11
  AND phone ~ '^[0-9]{11}$';