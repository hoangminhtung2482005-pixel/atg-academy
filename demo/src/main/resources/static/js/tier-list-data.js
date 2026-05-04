const heroes = [];

const TIER_HERO_FALLBACK_IMAGE = '/images/ui/default.png';
const TIER_HERO_IMAGE_FILES = [
    'Airi.jpg',
    'Aleister.jpg',
    'Alice.png',
    'Allain.jpg',
    'Amily.jpg',
    'Annette.jpg',
    'Aoi.jpeg',
    'Arduin.jpg',
    'arthur.jpg',
    'Arum.jpg',
    'Astrid.jpg',
    'Ata.jpg',
    'Aya.jpg',
    'azzen-ka.jpg',
    'Baldum.jpg',
    'Bijan.jpg',
    'Billow.jpg',
    'Biron.jpg',
    'bolt-baron.jpg',
    'Bonnie.jpg',
    'Bright.jpg',
    'Butterfly.jpg',
    'Capheny.jpg',
    'Celica.jpg',
    'Charlotte.jpg',
    'Chaugnar.jpg',
    'Cresht.jpg',
    'd-arcy.jpg',
    'Dextra.jpg',
    'dieu-thuyen.jpg',
    'Dirak.jpg',
    'Dolia.jpg',
    'Dyadia.jpg',
    'Edras.jpg',
    'eland-orr.jpg',
    'Elsu.jpg',
    'Enzo.jpg',
    'Erin.jpg',
    'Errol.jpg',
    'Fennik.jpg',
    'Florentino.jpg',
    'flowborn-mage.jpg',
    'flowborn-marksman.jpg',
    'Gildur.jpg',
    'Goverra.jpg',
    'Grakk.png',
    'Hayate.jpg',
    'Heino.jpg',
    'Helen.jpg',
    'Iggy.jpeg',
    'Ignis.jpg',
    'Ilumia.jpg',
    'Ishar.jpg',
    'Jinna.jpg',
    'Kahlii.png',
    'Kaine.jpg',
    'Keera.jpg',
    'kil-groth.gif',
    'Kriknak.png',
    'Krixi.png',
    'Krizzix.png',
    'Lauriel.jpg',
    'Laville.jpg',
    'Liliana.jpg',
    'Lindis.jpg',
    'Lorion.jpg',
    'lu-bu.jpg',
    'Lumburr.jpg',
    'Maloch.jpg',
    'Marja.jpg',
    'Max.jpg',
    'Mganga.jpg',
    'Mina.png',
    'Ming.jpg',
    'Moren.jpg',
    'Murad.jpg',
    'Nakroth.jpg',
    'Natalya.jpg',
    'ngo-khong.jpg',
    'Omega.jpg',
    'Omen.jpg',
    'ormarr.jpg',
    'Paine.jpg',
    'Preyta.jpg',
    'Qi.jpg',
    'Quillen.jpg',
    'Raz.jpg',
    'Richter.jpg',
    'Rouie.jpg',
    'Rourke.jpg',
    'Roxie.jpg',
    'Ryoma.jpg',
    'Sephera.jpg',
    'Sinestrea.jpg',
    'Skud.jpg',
    'Slimz.png',
    'Stuart.jpg',
    'Superman.jpg',
    'Taara.jpg',
    'Tachi.jpg',
    'TeeMee.jpg',
    'Teeri.jpg',
    'tel-annas.jpg',
    'Thane.png',
    'the-flash.jpg',
    'thorne.jpg',
    'Toro.jpg',
    'trieu-van.jpg',
    'Tulen.jpg',
    'Valhein.jpg',
    'Veera.jpg',
    'Veres.jpg',
    'Violet.jpg',
    'Volkath.jpg',
    'Wiro.jpg',
    'Wisp.jpg',
    'wonder-woman.jpg',
    'Xeniel.jpg',
    'Yan.jpg',
    'y-bneth.jpg',
    'Yena.jpg',
    'Yorn.jpg',
    'Yue.jpg',
    'Zata.jpg',
    'zephys.jpg',
    'Zill.jpg',
    'Zip.jpg',
    'Zuka.jpg'
];
const HERO_IMAGE_ALIASES = Object.freeze({
    'azzen-ka': 'azzen-ka.jpg',
    'd-arcy': 'd-arcy.jpg',
    'diaochan': 'dieu-thuyen.jpg',
    'dieu-thuyen': 'dieu-thuyen.jpg',
    'flowborn': 'flowborn-marksman.jpg',
    'flowborn-adl': 'flowborn-marksman.jpg',
    'flowborn-mage': 'flowborn-mage.jpg',
    'flowborn-marksman': 'flowborn-marksman.jpg',
    'flowborn-mid': 'flowborn-mage.jpg',
    'kahli': 'Kahlii.png',
    'kahlii': 'Kahlii.png',
    'kil-groth': 'kil-groth.gif',
    'kilgroth': 'kil-groth.gif',
    'lu-bu': 'lu-bu.jpg',
    'ngo-khong': 'ngo-khong.jpg',
    'omarr': 'ormarr.jpg',
    'ormarr': 'ormarr.jpg',
    'richter': 'Richter.jpg',
    'riktor': 'Richter.jpg',
    'rourka': 'Rourke.jpg',
    'rourke': 'Rourke.jpg',
    'tel-annas': 'tel-annas.jpg',
    'the-flash': 'the-flash.jpg',
    'thorne': 'thorne.jpg',
    'trieu-van': 'trieu-van.jpg',
    'wonder-woman': 'wonder-woman.jpg',
    'wukong': 'ngo-khong.jpg',
    'y-bneth': 'y-bneth.jpg',
    'ybneth': 'y-bneth.jpg',
    'zanis': 'trieu-van.jpg',
    'zephys': 'zephys.jpg'
});
const heroImageMap = HERO_IMAGE_ALIASES;

const HERO_API = '/api/wiki/heroes';
const TIER_ROLE_ORDER = Object.freeze(['DSL', 'JGL', 'MID', 'ADL', 'SUP']);
const TIER_LANE_ROLE_LABELS = Object.freeze({
    DSL: 'Đường Tà Thần',
    JGL: 'Đi Rừng',
    MID: 'Đường Giữa',
    ADL: 'Đường Rồng',
    SUP: 'Trợ Thủ'
});
const heroByIdMap = new Map();
const heroByNameMap = new Map();
const heroBySlugMap = new Map();
const heroAssetFileByKey = new Map();
const reportedHeroImageFailures = new Set();

TIER_HERO_IMAGE_FILES.forEach(file => {
    const key = toHeroAssetKey(stripHeroFileExtension(file));
    if (key && !heroAssetFileByKey.has(key)) {
        heroAssetFileByKey.set(key, file);
    }
});
Object.entries(HERO_IMAGE_ALIASES).forEach(([key, file]) => {
    if (key && file) {
        heroAssetFileByKey.set(key, file);
    }
});

function stripHeroFileExtension(value) {
    return String(value || '').replace(/\.[^.]+$/, '');
}

function decodeHeroUrlSegment(value) {
    try {
        return decodeURIComponent(value);
    } catch (error) {
        return value;
    }
}

function encodeUrlPathSegments(pathname) {
    return pathname
        .split('/')
        .map((segment, index) => (index === 0 ? '' : encodeURIComponent(decodeHeroUrlSegment(segment))))
        .join('/');
}

function toHeroAssetKey(value) {
    return String(value ?? '')
        .replace(/[’‘`´]/g, "'")
        .normalize('NFD')
        .replace(/[\u0300-\u036f]/g, '')
        .replace(/[đĐ]/g, 'd')
        .toLowerCase()
        .replace(/[^a-z0-9]+/g, '-')
        .replace(/^-+|-+$/g, '');
}

function normalizeHeroName(name) {
    const aliases = {
        Flowborn: 'Flowborn (Marksman)',
        'Flowborn ADL': 'Flowborn (Marksman)',
        'Flowborn MID': 'Flowborn (Mage)',
        'Ngo Khong': 'Ng\u1ed9 Kh\u00f4ng',
        Wukong: 'Ng\u1ed9 Kh\u00f4ng',
        'Trieu Van': 'Tri\u1ec7u V\u00e2n',
        Zanis: 'Tri\u1ec7u V\u00e2n',
        'Dieu Thuyen': '\u0110i\u00eau Thuy\u1ec1n',
        Diaochan: '\u0110i\u00eau Thuy\u1ec1n',
        'Lu Bo': 'Lu Bu',
        Roule: 'Rouie',
        Governa: 'Goverra',
        Tochi: 'Tachi',
        Richter: 'Riktor',
        Rourka: 'Rourke',
        Omarr: 'Ormarr',
        Kahli: 'Kahlii',
        KilGroth: "Kil'Groth",
        'Wonder Women': 'Wonder Woman'
    };
    const trimmed = String(name || '')
        .replace(/[’‘`´]/g, "'")
        .trim();
    return aliases[trimmed] || trimmed;
}

function normalizeTierRoleText(value) {
    return String(value ?? '')
        .replace(/[’‘`´]/g, "'")
        .normalize('NFD')
        .replace(/[\u0300-\u036f]/g, '')
        .replace(/[đĐ]/g, 'd')
        .toLowerCase()
        .replace(/[^a-z0-9]+/g, ' ')
        .trim();
}

function getTierColumnRoleCode(column) {
    const rawValues = [
        column?.label,
        column?.alt,
        column?.code,
        column?.role,
        column?.roleCode,
        column?.name
    ];

    for (const value of rawValues) {
        const upper = String(value ?? '').trim().toUpperCase();
        if (TIER_ROLE_ORDER.includes(upper)) return upper;
    }

    const normalized = rawValues.map(normalizeTierRoleText).filter(Boolean).join(' ');
    if (!normalized) return '';
    if (/\bdsl\b|\btop\b|solo|ta than|duong ta than/.test(normalized)) return 'DSL';
    if (/\bjgl\b|\bjg\b|jungle|rung|di rung/.test(normalized)) return 'JGL';
    if (/\bmid\b|giua|duong giua/.test(normalized)) return 'MID';
    if (/\badl\b|\badc\b|\bad\b|xa thu|marksman|duong rong/.test(normalized)) return 'ADL';
    if (/\bsup\b|\bsp\b|support|tro thu|ho tro|do don|tank/.test(normalized)) return 'SUP';
    return '';
}

function normalizeTierRoleColumnOrder(contentData) {
    if (!contentData) return contentData;
    let data = contentData;
    if (typeof data === 'string') {
        try {
            data = JSON.parse(data);
        } catch (error) {
            return contentData;
        }
    }
    if (!data || !Array.isArray(data.columns) || !Array.isArray(data.rows)) {
        return data;
    }

    const columns = data.columns.map((column, index) => ({
        column,
        index,
        roleCode: getTierColumnRoleCode(column)
    }));
    const knownColumns = TIER_ROLE_ORDER
        .map(roleCode => columns.find(item => item.roleCode === roleCode))
        .filter(Boolean);
    const unknownColumns = columns.filter(item => !item.roleCode || !TIER_ROLE_ORDER.includes(item.roleCode));
    const orderedColumns = knownColumns.concat(unknownColumns);

    if (orderedColumns.length !== columns.length) {
        return data;
    }

    return {
        ...data,
        columns: orderedColumns.map(item => item.column),
        rows: data.rows.map(row => ({
            ...row,
            cells: orderedColumns.map(item => Array.isArray(row.cells?.[item.index]) ? row.cells[item.index] : [])
        }))
    };
}

function normalizeHeroImageUrl(url, options = {}) {
    const absolute = options.absolute === true;
    if (!url) {
        return absolute ? new URL(TIER_HERO_FALLBACK_IMAGE, window.location.origin).href : TIER_HERO_FALLBACK_IMAGE;
    }
    try {
        const parsed = new URL(url, window.location.origin);
        if (parsed.protocol === 'data:' || parsed.protocol === 'blob:') {
            return url;
        }
        const encodedPath = encodeUrlPathSegments(parsed.pathname);
        const pathWithSearch = `${encodedPath}${parsed.search}`;
        if (absolute || parsed.origin !== window.location.origin) {
            return `${parsed.origin}${pathWithSearch}`;
        }
        return pathWithSearch;
    } catch (error) {
        return String(url);
    }
}

function getHeroImagePathname(url) {
    try {
        return new URL(url, window.location.origin).pathname;
    } catch (error) {
        return String(url || '').split(/[?#]/)[0];
    }
}

function isDefaultHeroImageUrl(url) {
    const pathname = getHeroImagePathname(url);
    return pathname === TIER_HERO_FALLBACK_IMAGE || pathname === '/images/ui/logo.png';
}

function extractHeroAssetName(url) {
    if (!url) return '';
    try {
        const parsed = new URL(url, window.location.origin);
        const parts = parsed.pathname.split('/').filter(Boolean);
        return parts.length ? decodeHeroUrlSegment(parts[parts.length - 1]) : '';
    } catch (error) {
        const clean = String(url).split(/[?#]/)[0];
        const parts = clean.split('/').filter(Boolean);
        return parts.length ? decodeHeroUrlSegment(parts[parts.length - 1]) : '';
    }
}

function findHeroAssetFilename(value) {
    const candidates = new Set();
    const addCandidate = candidate => {
        const text = String(candidate || '').trim();
        if (!text) return;
        candidates.add(text);
        const normalized = normalizeHeroName(text);
        if (normalized && normalized !== text) {
            candidates.add(normalized);
        }
        const assetName = extractHeroAssetName(text);
        if (assetName && assetName !== text) {
            candidates.add(assetName);
            candidates.add(stripHeroFileExtension(assetName));
        }
    };

    const catalogHero = getHeroFromValue(value);
    if (catalogHero) {
        addCandidate(catalogHero.slug);
        addCandidate(catalogHero.name);
        addCandidate(catalogHero.avatarUrl);
    }

    if (value && typeof value === 'object') {
        addCandidate(value.slug);
        addCandidate(value.heroSlug);
        addCandidate(value.name);
        addCandidate(value.heroName);
        addCandidate(value.avatarUrl);
        addCandidate(value.portraitUrl);
        addCandidate(value.imageUrl);
    } else {
        addCandidate(value);
    }

    for (const candidate of candidates) {
        const key = toHeroAssetKey(stripHeroFileExtension(candidate));
        if (key && heroAssetFileByKey.has(key)) {
            return heroAssetFileByKey.get(key);
        }
    }
    return '';
}

function buildHeroAssetUrl(filename, options = {}) {
    return normalizeHeroImageUrl(`/images/heroes/${String(filename || '').trim()}`, options);
}

function getHeroImageSource(value, catalogHero) {
    if (typeof value === 'string' && /^(?:https?:)?\/\//i.test(value)) {
        return value;
    }
    if (typeof value === 'string' && value.startsWith('/')) {
        return value;
    }
    if (value && typeof value === 'object') {
        return value.avatarUrl || value.portraitUrl || value.imageUrl || catalogHero?.avatarUrl || '';
    }
    return catalogHero?.avatarUrl || '';
}

function resolveHeroImageUrl(value, options = {}) {
    const absolute = options.absolute === true;
    const preferLocal = options.preferLocal !== false;
    const localFile = findHeroAssetFilename(value);
    if (preferLocal && localFile) {
        return buildHeroAssetUrl(localFile, { absolute });
    }

    const catalogHero = getHeroFromValue(value);
    const rawUrl = getHeroImageSource(value, catalogHero);
    if (rawUrl && !isDefaultHeroImageUrl(rawUrl)) {
        return normalizeHeroImageUrl(rawUrl, { absolute });
    }

    if (localFile) {
        return buildHeroAssetUrl(localFile, { absolute });
    }

    return normalizeHeroImageUrl(TIER_HERO_FALLBACK_IMAGE, { absolute });
}

function warnHeroImageFailure(heroName, url) {
    const key = `${String(heroName || '').trim()}::${String(url || '').trim()}`;
    if (reportedHeroImageFailures.has(key)) return;
    reportedHeroImageFailures.add(key);
    console.warn('Hero image failed:', heroName || 'Unknown hero', url || '');
}

function handleTierHeroImageError(img, heroName, fallbackUrl = TIER_HERO_FALLBACK_IMAGE) {
    const failedUrl = img?.currentSrc || img?.src || '';
    warnHeroImageFailure(heroName || img?.dataset?.heroName || img?.alt || '', failedUrl);
    if (!img) return;

    const fallback = normalizeHeroImageUrl(fallbackUrl);
    if (normalizeHeroImageUrl(failedUrl, { absolute: true }) === normalizeHeroImageUrl(fallback, { absolute: true })) {
        img.onerror = null;
        return;
    }

    img.onerror = null;
    img.src = fallback;
}

function rebuildHeroIndexes() {
    heroByIdMap.clear();
    heroByNameMap.clear();
    heroBySlugMap.clear();
    heroes.forEach(hero => {
        if (hero.id !== undefined && hero.id !== null) {
            heroByIdMap.set(String(hero.id), hero);
        }
        if (hero.name) {
            heroByNameMap.set(normalizeHeroName(hero.name), hero);
        }
        if (hero.slug) {
            heroBySlugMap.set(toHeroAssetKey(hero.slug), hero);
        }
    });
}

async function loadHeroesFromApi() {
    heroes.length = 0;
    heroByIdMap.clear();
    heroByNameMap.clear();
    heroBySlugMap.clear();
    try {
        const response = await fetch(HERO_API, { headers: { Accept: 'application/json' }, cache: 'no-store' });
        if (!response.ok) throw new Error(`HTTP ${response.status}`);
        const data = await response.json();
        if (!Array.isArray(data)) throw new Error('Invalid hero payload');
        data.forEach(hero => {
            const primaryRole = getHeroPrimaryRole(hero);
            const primaryRoleCode = primaryRole?.code || '';
            heroes.push({
                id: hero.id,
                slug: hero.slug || '',
                name: hero.name,
                role: primaryRoleCode,
                roleName: getHeroPrimaryRoleName(hero),
                primaryRole,
                primaryRoleCode,
                subRoles: getHeroSubRoles(hero),
                heroClass: hero.heroClass || '',
                classes: getHeroClassNames(hero),
                laneRoles: getHeroLaneRoleLabels(hero),
                attributes: Array.isArray(hero.attributes) ? hero.attributes : [],
                avatarUrl: hero.avatarUrl || ''
            });
        });
        rebuildHeroIndexes();
    } catch (error) {
        console.error('Cannot load hero catalog:', error);
    }
}

function getHeroFromValue(value) {
    if (value && typeof value === 'object') {
        const id = value.heroId ?? value.id;
        if (id !== undefined && id !== null && heroByIdMap.has(String(id))) {
            return heroByIdMap.get(String(id));
        }

        const slug = value.slug || value.heroSlug;
        if (slug) {
            const slugKey = toHeroAssetKey(slug);
            if (heroBySlugMap.has(slugKey)) {
                return heroBySlugMap.get(slugKey);
            }
        }

        const name = value.name || value.heroName;
        if (name && heroByNameMap.has(normalizeHeroName(name))) {
            return heroByNameMap.get(normalizeHeroName(name));
        }
    }

    if (typeof value === 'number' && heroByIdMap.has(String(value))) {
        return heroByIdMap.get(String(value));
    }

    if (typeof value === 'string') {
        if (/^\d+$/.test(value) && heroByIdMap.has(value)) {
            return heroByIdMap.get(value);
        }

        const slugKey = toHeroAssetKey(value);
        if (slugKey && heroBySlugMap.has(slugKey)) {
            return heroBySlugMap.get(slugKey);
        }

        const normalizedName = normalizeHeroName(value);
        if (heroByNameMap.has(normalizedName)) {
            return heroByNameMap.get(normalizedName);
        }
    }

    return null;
}

function getHeroNameFromValue(value) {
    const hero = getHeroFromValue(value);
    if (hero) return hero.name;
    if (value && typeof value === 'object') {
        return String(value.name || value.heroName || `Hero #${value.heroId ?? value.id ?? ''}`)
            .trim();
    }
    return String(value || '').trim();
}

function getHeroIdFromValue(value) {
    const hero = getHeroFromValue(value);
    if (hero?.id !== undefined && hero.id !== null) return hero.id;
    if (value && typeof value === 'object') return value.heroId ?? value.id ?? null;
    if (typeof value === 'number') return value;
    if (typeof value === 'string' && /^\d+$/.test(value)) return Number(value);
    return null;
}

function normalizeHeroFilterText(value) {
    return String(value || '')
        .normalize('NFD')
        .replace(/[\u0300-\u036f]/g, '')
        .toLowerCase()
        .replace(/đ/g, 'd')
        .replace(/[^a-z0-9]+/g, ' ')
        .trim();
}

function getHeroClassNames(hero) {
    const names = [];
    const pushClass = value => {
        if (!value) return;
        if (typeof value === 'string') {
            if (value.trim()) names.push(value.trim());
            return;
        }
        const label = value.displayName || value.name || value.code || value.className || value.heroClass;
        if (label && String(label).trim()) names.push(String(label).trim());
    };

    if (Array.isArray(hero?.classes)) hero.classes.forEach(pushClass);
    if (Array.isArray(hero?.heroClasses)) hero.heroClasses.forEach(pushClass);
    pushClass(hero?.heroClass);
    pushClass(hero?.className);

    const seen = new Set();
    return names.filter(name => {
        const key = normalizeHeroFilterText(name);
        if (!key || seen.has(key)) return false;
        seen.add(key);
        return true;
    });
}

function normalizeTierLaneRoleCode(value) {
    const raw = String(value || '').trim().toUpperCase();
    if (Object.prototype.hasOwnProperty.call(TIER_LANE_ROLE_LABELS, raw)) {
        return raw;
    }
    const text = String(value || '')
        .normalize('NFD')
        .replace(/[\u0300-\u036f]/g, '')
        .toLowerCase();
    if (text.includes('dsl') || text.includes('solo') || text.includes('top') || text.includes('ta than')) return 'DSL';
    if (text.includes('jgl') || text.includes('rung') || text.includes('jungle')) return 'JGL';
    if (text.includes('mid') || text.includes('giua')) return 'MID';
    if (text.includes('adl') || text.includes('rong') || text.includes('xa thu') || text.includes('marksman')) return 'ADL';
    if (text.includes('sup') || text.includes('tro thu') || text.includes('ho tro') || text.includes('support')) return 'SUP';
    return '';
}

function normalizeHeroRoleObject(role) {
    if (!role) return null;
    if (typeof role === 'string') {
        const code = normalizeTierLaneRoleCode(role);
        return code ? { code, name: TIER_LANE_ROLE_LABELS[code] || role } : null;
    }
    const code = normalizeTierLaneRoleCode(role.code || role.name);
    if (!code && !role.name) return null;
    return {
        id: role.id ?? null,
        code: code || role.code || '',
        name: role.name || TIER_LANE_ROLE_LABELS[code] || code
    };
}

function getHeroPrimaryRole(hero) {
    if (!hero) return null;
    const primaryRole = normalizeHeroRoleObject(hero.primaryRole);
    if (primaryRole) return primaryRole;
    if (Array.isArray(hero.roles) && hero.roles.length) {
        return normalizeHeroRoleObject(hero.roles[0]);
    }
    if (Array.isArray(hero.laneRoles) && hero.laneRoles.length) {
        return normalizeHeroRoleObject(hero.laneRoles[0]);
    }
    return null;
}

function getHeroPrimaryRoleCode(hero) {
    const primaryRole = getHeroPrimaryRole(hero);
    return primaryRole?.code || '';
}

function getHeroPrimaryRoleName(hero) {
    const primaryRole = getHeroPrimaryRole(hero);
    return primaryRole?.name || (primaryRole?.code ? TIER_LANE_ROLE_LABELS[primaryRole.code] : '') || '';
}

function getHeroSubRoles(hero) {
    if (!hero) return [];
    const primaryCode = getHeroPrimaryRoleCode(hero);
    if (Array.isArray(hero.subRoles)) {
        return hero.subRoles
            .map(normalizeHeroRoleObject)
            .filter(role => role && role.code !== primaryCode);
    }
    return (Array.isArray(hero.roles) ? hero.roles.slice(1) : [])
        .map(normalizeHeroRoleObject)
        .filter(role => role && role.code !== primaryCode);
}

function getHeroLaneRoleLabels(hero) {
    const labels = [];
    const primaryRoleName = getHeroPrimaryRoleName(hero);
    if (primaryRoleName) labels.push(primaryRoleName);
    getHeroSubRoles(hero).forEach(role => {
        if (role.name) labels.push(role.name);
    });
    return labels;
}

function getHeroRefForStorage(value) {
    const buildTempRef = (baseRef = {}) => {
        if (!value || typeof value !== 'object' || !value.tempInstance) return baseRef;
        const ref = { ...baseRef };
        ref.tempInstance = true;
        ref.instanceId = value.instanceId || `temp-${Date.now()}`;
        ref.sourceHeroId = value.sourceHeroId ?? value.heroId ?? value.id ?? ref.heroId ?? null;
        if (value.slug || value.heroSlug) ref.slug = value.slug || value.heroSlug;
        if (value.avatarUrl) ref.avatarUrl = value.avatarUrl;
        return ref;
    };

    const hero = getHeroFromValue(value);
    if (hero?.id) {
        const ref = { heroId: hero.id, name: hero.name };
        if (hero.slug) ref.slug = hero.slug;
        if (hero.avatarUrl) ref.avatarUrl = hero.avatarUrl;
        return buildTempRef(ref);
    }

    const name = getHeroNameFromValue(value);
    const id = getHeroIdFromValue(value);
    const ref = {};
    if (id) ref.heroId = id;
    if (name) ref.name = name;
    if (value && typeof value === 'object') {
        if (value.slug || value.heroSlug) ref.slug = value.slug || value.heroSlug;
        if (value.avatarUrl) ref.avatarUrl = value.avatarUrl;
    }
    return buildTempRef(ref);
}

function getHeroImgUrl(value, options = {}) {
    return resolveHeroImageUrl(value, options);
}

const DUMMY_COMMUNITY = [];
