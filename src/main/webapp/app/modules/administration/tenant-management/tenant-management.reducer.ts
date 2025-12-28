import axios from 'axios';
import { createAsyncThunk, createSlice } from '@reduxjs/toolkit';

import { ITenant } from './tenant.model';
import { FieldErrorVM } from 'app/shared/jhipster/problem-details';

const initialState = {
  loading: false,
  errorMessage: null,
  tenants: [] as ReadonlyArray<ITenant>,
  tenant: {} as ITenant,
  updating: false,
  updateSuccess: false,
  fieldErrors: [] as FieldErrorVM[],
};

export type TenantManagementState = Readonly<typeof initialState>;

// Actions
export const getTenants = createAsyncThunk('tenantManagement/fetch_tenants', async () => {
  const requestUrl = 'api/tenants';
  return axios.get<ITenant[]>(requestUrl);
});

export const getTenant = createAsyncThunk('tenantManagement/fetch_tenant', async (id: string | number) => {
  const requestUrl = `api/tenants/${id}`;
  return axios.get<ITenant>(requestUrl);
});

export const createTenant = createAsyncThunk('tenantManagement/create_tenant', async (tenant: ITenant) => {
  const result = await axios.post<ITenant>('api/tenants', tenant);
  return result;
});

export const updateTenant = createAsyncThunk('tenantManagement/update_tenant', async (tenant: ITenant) => {
  const result = await axios.put<ITenant>(`api/tenants/${tenant.id}`, tenant);
  return result;
});

export const deleteTenant = createAsyncThunk('tenantManagement/delete_tenant', async (id: string | number) => {
  const requestUrl = `api/tenants/${id}`;
  await axios.delete(requestUrl);
  return id;
});

export const TenantManagementSlice = createSlice({
  name: 'tenantManagement',
  initialState: initialState as TenantManagementState,
  reducers: {
    reset() {
      return initialState;
    },
  },
  extraReducers(builder) {
    builder
      .addCase(getTenants.pending, state => {
        state.loading = true;
      })
      .addCase(getTenants.fulfilled, (state, action) => {
        state.loading = false;
        state.tenants = action.payload.data;
      })
      .addCase(getTenants.rejected, (state, action) => {
        state.loading = false;
        state.errorMessage = action.error.message;
      })
      .addCase(getTenant.fulfilled, (state, action) => {
        state.loading = false;
        state.tenant = action.payload.data;
      })
      .addCase(createTenant.pending, state => {
        state.updating = true;
        state.updateSuccess = false;
      })
      .addCase(createTenant.fulfilled, state => {
        state.updating = false;
        state.updateSuccess = true;
        state.tenant = {};
        state.fieldErrors = [];
      })
      .addCase(createTenant.rejected, (state, action) => {
        state.updating = false;
        state.updateSuccess = false;
        // Extract field errors from the error response
        if (action.error && 'response' in action.error) {
          const axiosError = action.error as any;
          if (axiosError.response?.data?.fieldErrors) {
            state.fieldErrors = axiosError.response.data.fieldErrors;
          } else {
            state.fieldErrors = [];
          }
        } else {
          state.fieldErrors = [];
        }
      })
      .addCase(updateTenant.pending, state => {
        state.updating = true;
        state.updateSuccess = false;
      })
      .addCase(updateTenant.fulfilled, state => {
        state.updating = false;
        state.updateSuccess = true;
        state.tenant = {};
        state.fieldErrors = [];
      })
      .addCase(updateTenant.rejected, (state, action) => {
        state.updating = false;
        state.updateSuccess = false;
        // Extract field errors from the error response
        if (action.error && 'response' in action.error) {
          const axiosError = action.error as any;
          if (axiosError.response?.data?.fieldErrors) {
            state.fieldErrors = axiosError.response.data.fieldErrors;
          } else {
            state.fieldErrors = [];
          }
        } else {
          state.fieldErrors = [];
        }
      })
      .addCase(deleteTenant.pending, state => {
        state.updating = true;
      })
      .addCase(deleteTenant.fulfilled, state => {
        state.updating = false;
        state.updateSuccess = true;
        state.tenant = {};
      })
      .addCase(deleteTenant.rejected, state => {
        state.updating = false;
        state.updateSuccess = false;
      });
  },
});

export const { reset } = TenantManagementSlice.actions;

export default TenantManagementSlice.reducer;
